package cn.lwx.rlstg.algorithm.QNetwork;


import cn.lwx.rlstg.GlobalManager;
import cn.lwx.rlstg.algorithm.Common.Vector2D;
import cn.lwx.rlstg.algorithm.Controller;
import cn.lwx.rlstg.algorithm.QLearning.QState;
import cn.lwx.rlstg.gameobjects.Bullet;
import org.encog.engine.network.activation.ActivationReLU;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Package: cn.lwx.rlstg.algorithm.QNetwork
 * Comments:
 * Author: lwx
 * Create Date: 2018/1/28
 * Modified Date: 2018/2/3
 * Why & What is modified:
 * Version: 1.2.2
 * It's the only NEET thing to do. – Shionji Yuuko
 */
public class QNetwork extends Controller {
    //hyper parameters
    private static final double GAMMA = 0.9;//discount factor for target Q
    private static final double EPSILON = 0.8;//final value of epsilon
    private static final int REPLAY_SIZE = 32;//experience replay buffer size
    private static final int MAX_ENEMY_COUNT = 4;
    private static final int MAX_BULLET_COUNT = 4;

    public static final int INPUT_SIZE = MAX_ENEMY_COUNT * 2 + MAX_BULLET_COUNT * 2;

    private BasicNetwork qNetwork;
    private QState nextState;
    private QState nowState;
    private boolean isTrainingDone;
    private double reward;

    private ArrayList<ArrayList<Double>> trainDataInput;
    private ArrayList<ArrayList<Double>> trainDataOutput;

    public QNetwork() {
        super(Controller.ALGORITHM_QNETWORK);
        qNetwork = new BasicNetwork();
        //add layer
        //parameter 1 means activation function, parameter 2 means bias, parameter 3 means neuron size
        //input layer have 16 neurons
        qNetwork.addLayer(new BasicLayer(null, true, INPUT_SIZE));
        qNetwork.addLayer(new BasicLayer(new ActivationReLU(-15,-15), true, 20));
        qNetwork.addLayer(new BasicLayer(new ActivationReLU(-15,-15), true, 10));
        //output layer have 5 neurons
        qNetwork.addLayer(new BasicLayer(new ActivationReLU(-15,-15), false, GlobalManager.ACTION_COUNT));

        //finalizeStructure method is called to inform the network that no more layers are to be added
        qNetwork.getStructure().finalizeStructure();
        qNetwork.reset();

        trainDataOutput = new ArrayList<>();
        trainDataInput = new ArrayList<>();

        isTrainingDone = false;
        nowState = new QState();
        nextState = new QState();
        reward = 0;
    }

    @Override
    public int decide() {
        if (isTrainingDone || Math.random() < EPSILON)
            return getActionFromData(qNetwork.compute(stateToData(nowState)));
        return (int) (Math.random() * GlobalManager.ACTION_COUNT);
    }

    public void train() {
        double[][] inputData = new double[REPLAY_SIZE][INPUT_SIZE];
        double[][] outputData = new double[REPLAY_SIZE][GlobalManager.ACTION_COUNT];

        for (int i = 0; i < REPLAY_SIZE; i++) {
            for (int j = 0; j < INPUT_SIZE; j++) {
                inputData[i][j] = trainDataInput.get(i).get(j);
            }
        }

        for (int i = 0; i < REPLAY_SIZE; i++) {
            for (int j = 0; j < GlobalManager.ACTION_COUNT; j++) {
                outputData[i][j] = trainDataOutput.get(i).get(j);
            }
        }

        //init train set
        MLDataSet trainingSet = new BasicMLDataSet(inputData,outputData);

        //init train method
        ResilientPropagation train = new ResilientPropagation (qNetwork, trainingSet);

        for (int i = 0; i < REPLAY_SIZE; i++) {
            train.iteration();
        }

        if (train.getError() < 0.001) {
            isTrainingDone = true;
        }

        trainingSet.close();
        trainDataInput.clear();
        trainDataOutput.clear();
    }

    public void updateState(int action){
        //add all enemies to list and sort by distance
        ArrayList<Vector2D> enemyVectors = new ArrayList<>();
        GlobalManager.GLOBAL_MANAGER.getEnemies().forEach(enemy -> {
            enemyVectors.add(new Vector2D(GlobalManager.GLOBAL_MANAGER.getPlayer().getX() - enemy.getX(),
                    GlobalManager.GLOBAL_MANAGER.getPlayer().getY() - enemy.getY()));
        });
        Collections.sort(enemyVectors);

        //if list have more enemies, keep shortest and delete others.
        if(enemyVectors.size() > MAX_ENEMY_COUNT) {
            ArrayList<Vector2D> removeList = new ArrayList<>();
            int maxDistance = enemyVectors.get(MAX_ENEMY_COUNT - 1).getDistance();
            enemyVectors.forEach(bullet->{
                if(bullet.getDistance() > maxDistance)
                    removeList.add(bullet);
            });
            enemyVectors.removeAll(removeList);
        }

        //add all bullets to list and sort by distance
        ArrayList<Vector2D> bulletVectors = new ArrayList<>();
        GlobalManager.GLOBAL_MANAGER.getBullets().forEach(bullet -> {
            if(bullet.getFlag() == Bullet.PARENTS_ENEMY)
                bulletVectors.add(new Vector2D(GlobalManager.GLOBAL_MANAGER.getPlayer().getX() - bullet.getX(),
                        GlobalManager.GLOBAL_MANAGER.getPlayer().getY() - bullet.getY()));
        });
        Collections.sort(bulletVectors);

        //if list have more bullets, keep shortest and delete others.
        if(bulletVectors.size() > MAX_BULLET_COUNT) {
            ArrayList<Vector2D> removeList = new ArrayList<>();
            int maxDistance = bulletVectors.get(MAX_BULLET_COUNT - 1).getDistance();
            bulletVectors.forEach(bullet->{
                if(bullet.getDistance() > maxDistance)
                    removeList.add(bullet);
            });
            bulletVectors.removeAll(removeList);
        }

        //update state
        nowState = nextState;
        nextState = new QState(bulletVectors,enemyVectors);

        //add state to training set
        makeTrainingData(action);

        //check for train
        checkReplay();

        //reset reward
        reward = 0;
    }

    public void clearTrainingData(){
        nowState = new QState();
        nextState = new QState();
    }

    private void checkReplay() {
        if (trainDataInput.size() >= REPLAY_SIZE){
            this.train();
        }
    }

    public void gainReward(double reward){
        this.reward += reward;
    }

    public void makeTrainingData(int action){
        if (nextState.getLists().size()>0 && nowState.getLists().size()>0){
            trainDataInput.add(nowState.stateToArray());
            ArrayList<Double> outputList = new ArrayList<>();
            MLData output = qNetwork.compute(nextState.stateToData());
            double qvalue = output.getData(getActionFromData(output));
            for (int i = 0; i < output.size(); i++) {
                if (i == action)
                    outputList.add(reward + GAMMA * qvalue);
                else
                    outputList.add(GAMMA * qvalue);
            }
            trainDataOutput.add(outputList);
        }
    }

    private MLData stateToData(QState state) {
        return state.stateToData();
    }

    private int getActionFromData(MLData data) {
        int index = 0;
        for (int i = 0; i < data.size(); i++) {
            if (data.getData(index) < data.getData(i)) {
                index = i;
            }
        }
        return index;
    }
}
