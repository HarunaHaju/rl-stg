package cn.lwx.rlstg.gameobjects;

import cn.lwx.rlstg.GlobalManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Package: cn.lwx.rlstg.gameobjects
 * Comments:
 * Author: lwx
 * Create Date: 2017/12/18
 * Modified Date: 2017/12/25
 * Why & What is modified:
 * Version: 0.0.1beta
 * It's the only NEET thing to do. – Shionji Yuuko
 */
public class Enemy extends CommonObjects {

    private BufferedImage image;

    public Enemy(){
        super(100,50,50,5,10);
        try {
            image = ImageIO.read(Enemy.class.getResource("/img/enemy.png"));
            this.setWidth(image.getWidth());
            this.setHeight(image.getHeight());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public Enemy(double hp, int x, int y, int speed, double damage) {
        super(hp, x, y, speed, damage);
        try {
            image = ImageIO.read(Enemy.class.getResource("/img/enemy.png"));
            this.setWidth(image.getWidth());
            this.setHeight(image.getHeight());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void shot(){
        Bullet bullet = new Bullet(this.getX() + this.getWidth()/2,this.getY()
                ,this.getSpeed(),this.getDamage(),Bullet.PARENTS_ENEMY);
        GlobalManager.GLOBAL_MANAGER.getBullets().add(bullet);
    }

    @Override
    public void step() {

    }

    public BufferedImage getImage() {
        if(image.getHeight()>0&&image.getWidth()>0)
            return image;
        else
            return null;
    }
}
