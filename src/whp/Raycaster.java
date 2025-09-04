package whp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.*;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.*;

public class Raycaster extends JPanel implements KeyListener, Runnable {
    final int screenWidth = 1800;
    final int screenHeight = 1080;

    final int mapWidth = 24;
    final int mapHeight = 24;

    int[][] worldMap = {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,2,0,0,0,0,1,0,0,0,0,0,1,1,1,1,1,1,1,0,0,0,0,1},
            {1,0,1,0,0,0,2,0,0,0,0,0,1,8,0,0,0,0,1,1,1,1,1,1},
            {1,0,0,0,0,0,1,1,1,1,1,1,1,1,0,0,0,0,1,0,0,6,1,1},
            {1,0,0,0,0,0,2,1,6,0,0,0,6,0,1,0,0,0,0,0,0,0,1,1},
            {1,0,0,0,0,0,1,1,0,0,1,1,1,1,1,1,1,1,1,0,1,1,1,1},
            {1,0,0,0,0,0,2,0,0,0,1,0,2,0,0,0,0,0,1,0,1,0,0,1},
            {1,0,1,0,0,0,1,0,0,0,1,0,0,0,0,0,0,0,1,0,1,0,0,1},
            {1,0,1,0,0,0,2,0,0,0,1,0,0,0,0,1,0,0,1,0,1,0,0,1},
            {1,0,2,1,1,1,1,1,1,1,1,0,0,0,0,1,0,0,1,0,1,1,1,1},
            {1,0,1,1,0,0,0,0,0,0,1,0,0,0,0,1,0,0,1,0,0,0,1,1},
            {1,0,2,1,0,0,0,0,0,0,1,0,0,0,0,1,0,0,1,1,1,0,1,1},
            {1,0,1,0,0,0,0,0,0,0,1,0,0,0,0,1,0,0,0,2,1,0,1,1},
            {1,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,0,0,1,1,1,0,1,1},
            {1,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,1,1},
            {1,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,1,1},
            {1,0,0,0,1,0,2,1,1,1,1,1,1,1,1,1,0,0,1,1,1,1,1,1},
            {1,0,0,0,1,1,1,0,0,0,0,0,0,0,0,1,0,0,1,0,0,0,0,1},
            {1,0,0,0,0,0,2,0,0,0,0,0,1,0,0,1,0,0,1,1,0,0,0,1},
            {1,0,0,0,2,0,0,0,0,0,0,0,1,0,0,1,0,0,1,1,0,2,0,1},
            {1,0,2,0,0,0,2,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };

    double posX = 12, posY = 12;
    double dirX = -1, dirY = 0;
    double planeX = 0, planeY = 0.66;

    boolean[] keys = new boolean[256];
    BufferedImage screen;
    int[] pixels;

    static class Sprite {
        double x, y;
        int health;
        int damage;
        static String name;
        int level;
        BufferedImage tex;
        Sprite(double x, double y, BufferedImage tex, int health, int damage, String name, int level){
            this.x = x; this.y = y; this.tex = tex; this.health = health; this.damage = damage; this.name = name; this.level =level;
        }
    }

    boolean inBattle=false; Sprite battleEnemy; int playerHP=20, enemyHP; int playerLevel=1; int enemiesDefeated=0;
    double shakeX=0, shakeY=0;



    java.util.List<Sprite> enemies = new ArrayList<>();

    BufferedImage enemyTex;
    BufferedImage enemyTex1;
    BufferedImage enemyTex2;

    static class Move {
        String name;
        int damage;
        int heal;
        Move(String n, int d, int h) { name = n; damage = d; heal = h; }
    }
    Clip bgClip;
    Clip battleClip;

    java.util.List<Move> unlockedMoves = new ArrayList<>();
//main class
    public Raycaster() {
        screen = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) screen.getRaster().getDataBuffer()).getData();
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setFocusable(true);
        addKeyListener(this);

        try {
            enemyTex = ImageIO.read(getClass().getResource("/sprite2.png"));
            enemyTex1 = ImageIO.read(getClass().getResource("/skeleton.png"));
            enemyTex2 = ImageIO.read(getClass().getResource("/burber.png"));
        } catch (IOException e) { e.printStackTrace(); }
// für ein neuen gegenr kopier ein for loop change die vorvor letzen line ints zu r ächsten zahl neue gegener gehören ganz oben Kopiere hier!

// gegner spawnen (burger)
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (x >= 0 && x < worldMap.length && y >= 0 && y < worldMap[0].length) {
                    if (worldMap[x][y] >= 8 && worldMap[x][y] <= 9) {
                        enemies.add(new Sprite(x + 0.5, y + 0.5, enemyTex2, 50,10,"Burger",10));
                        worldMap[x][y] = 0;
                    }
                }
            }
        }

// gegner spawnen (Baum bro)
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (x >= 0 && x < worldMap.length && y >= 0 && y < worldMap[0].length) {
                    if (worldMap[x][y] >= 6 && worldMap[x][y] <= 7) {
                        enemies.add(new Sprite(x + 0.5, y + 0.5, enemyTex, 20,8,"Treeguy",5));
                        worldMap[x][y] = 0;
                    }
                }
            }
        }
        // gegner spawnen (Skelettkopf)
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (x >= 0 && x < worldMap.length && y >= 0 && y < worldMap[0].length) {
                    if (worldMap[x][y] >= 2 && worldMap[x][y] <= 4) {
                        enemies.add(new Sprite(x + 0.5, y + 0.5, enemyTex1, 9,5,"Skeletonhead",2));
                        worldMap[x][y] = 0;
                    }
                }
            }
        }




        unlockedMoves.add(new Move("Attack", 5, 0));
        unlockedMoves.add(new Move("Heal", 0, 5));
    }
    private Clip loadClip(String path){
        try{
            URL url = getClass().getResource(path);
            if(url==null){ System.err.println("Sound not found: "+path); return null; }
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        }catch(Exception e){ e.printStackTrace(); return null; }
    }

    private void playBackground(String path){
        if(bgClip!=null && bgClip.isRunning()){ bgClip.stop(); bgClip.close(); }
        bgClip = loadClip(path);
        if(bgClip!=null) bgClip.loop(Clip.LOOP_CONTINUOUSLY);
    }
    private void playBattle(String path){
        if(bgClip!=null && bgClip.isRunning()){ bgClip.stop(); }
        if(battleClip!=null && battleClip.isRunning()){ battleClip.stop(); battleClip.close(); }
        battleClip = loadClip(path);
        if(battleClip!=null) battleClip.loop(Clip.LOOP_CONTINUOUSLY);
    }
    private void stopBattle(){
        if(battleClip!=null && battleClip.isRunning()){ battleClip.stop(); battleClip.close(); }
        if(bgClip!=null) bgClip.loop(Clip.LOOP_CONTINUOUSLY);
    }
    private void playEffect(String path){
        new Thread(() -> {
            Clip clip = loadClip(path);
            if(clip!=null) clip.start();
        }).start();
    }

//Battle UI
    @Override
    public void paintComponent(Graphics g){
        g.drawImage(screen,0,0,null);
        if(inBattle){
            g.setColor(Color.BLACK);
            g.fillRect(50, 50, 540, 380);
            g.setColor(Color.RED);
            g.drawString("BATTLE!", 300, 80);
            g.setColor(Color.WHITE);
            g.drawString("Player HP: " + playerHP, 70, 120);
            g.drawString("Enemy HP: " + enemyHP, 400, 120);
            g.drawString("Choose a move:", 150, 160);
            g.drawString("a level "+battleEnemy.level+" enemy has found you! "+"he deals on average "+battleEnemy.damage+" damage", 200, 100);

            // Show unlocked moves (NICHT ANFASSEN)
            for (int i = 0; i < unlockedMoves.size(); i++) {
                Move m = unlockedMoves.get(i);
                g.drawString((i+1) + ". " + m.name, 180, 190 + i*20);
            }
        }
    }
//render NICHT ANFASSEN
    public void render(){
        if(!inBattle){
            Arrays.fill(pixels, 0x87CEEB);
            for(int i=screenWidth*screenHeight/2;i<screenWidth*screenHeight;i++) pixels[i]=0x444444;
            double[] zBuffer = new double[screenWidth];


            for(int x=0;x<screenWidth;x++){
                double cameraX=2*x/(double)screenWidth-1;
                double rayDirX=dirX+planeX*cameraX;
                double rayDirY=dirY+planeY*cameraX;
                int mapX=(int)posX,mapY=(int)posY;
                double sideDistX,sideDistY;
                double deltaDistX=(rayDirX==0)?1e30:Math.abs(1/rayDirX);
                double deltaDistY=(rayDirY==0)?1e30:Math.abs(1/rayDirY);
                double perpWallDist;
                int stepX,stepY; boolean hit=false; int side=0;

                if(rayDirX<0){stepX=-1; sideDistX=(posX-mapX)*deltaDistX;}
                else {stepX=1; sideDistX=(mapX+1-posX)*deltaDistX;}
                if(rayDirY<0){stepY=-1; sideDistY=(posY-mapY)*deltaDistY;}
                else{stepY=1; sideDistY=(mapY+1-posY)*deltaDistY;}

                while(!hit){
                    if(sideDistX<sideDistY){sideDistX+=deltaDistX; mapX+=stepX; side=0;}
                    else{sideDistY+=deltaDistY; mapY+=stepY; side=1;}
                    if(worldMap[mapX][mapY]>0) hit=true;
                }

                if(side==0) perpWallDist=(mapX-posX+(1-stepX)/2)/rayDirX;
                else perpWallDist=(mapY-posY+(1-stepY)/2)/rayDirY;

                int lineHeight=(int)(screenHeight/perpWallDist);
                int drawStart=-lineHeight/2+screenHeight/2; if(drawStart<0) drawStart=0;
                int drawEnd=lineHeight/2+screenHeight/2; if(drawEnd>=screenHeight) drawEnd=screenHeight-1;

                int color=(side==1)?0x999999:0xAAAAAA;
                for(int y=drawStart;y<drawEnd;y++) pixels[y*screenWidth+x]=color;
                zBuffer[x]=perpWallDist;
            }


            for(Sprite s: enemies){
                double dx = posX - s.x;
                double dy = posY - s.y;
                double dist = Math.sqrt(dx*dx + dy*dy);


                boolean canSeePlayer = false;
                int steps = (int)(dist * 10);
                for(int i=1; i<=steps; i++){
                    double checkX = s.x + dx * i / steps;
                    double checkY = s.y + dy * i / steps;
                    if(worldMap[(int)checkX][(int)checkY] > 0){
                        canSeePlayer = false;
                        break;
                    }
                    canSeePlayer = true;
                }

                if(canSeePlayer && dist > 0.5){
                    double moveX = dx/dist * 0.02;
                    double moveY = dy/dist * 0.02;
                    if(worldMap[(int)(s.x+moveX)][(int)s.y] == 0) s.x += moveX;
                    if(worldMap[(int)s.x][(int)(s.y+moveY)] == 0) s.y += moveY;
                } else if(dist <= 0.5){
                    // trigger battle
                    inBattle = true;
                    battleEnemy = s;
                    enemyHP = s.health;
                    String name = s.name;
                    playBattle("/battle.wav");
                }
            }

//render garbage
            for(Sprite s: enemies){
                double spriteX=s.x-posX, spriteY=s.y-posY;
                double invDet=1.0/(planeX*dirY-dirX*planeY);
                double transformX=invDet*(dirY*spriteX-dirX*spriteY);
                double transformY=invDet*(-planeY*spriteX+planeX*spriteY);
                int spriteScreenX=(int)((screenWidth/2)*(1+transformX/transformY));
                int spriteHeight=Math.abs((int)(screenHeight/transformY));
                int drawStartY=-spriteHeight/2+screenHeight/2; if(drawStartY<0) drawStartY=0;
                int drawEndY=spriteHeight/2+screenHeight/2; if(drawEndY>=screenHeight) drawEndY=screenHeight-1;
                int spriteWidth=spriteHeight;
                int drawStartX=-spriteWidth/2+spriteScreenX; if(drawStartX<0) drawStartX=0;
                int drawEndX=spriteWidth/2+spriteScreenX; if(drawEndX>=screenWidth) drawEndX=screenWidth-1;

                for(int stripe=drawStartX; stripe<drawEndX; stripe++){
                    int texX=(int)(256*(stripe-(-spriteWidth/2+spriteScreenX))*s.tex.getWidth()/spriteWidth)/256;
                    if(transformY>0 && stripe>0 && stripe<screenWidth && transformY<zBuffer[stripe]){
                        for(int y=drawStartY;y<drawEndY;y++){
                            int d=(y-screenHeight/2+spriteHeight/2)*256;
                            int texY=(d*s.tex.getHeight()/spriteHeight)/256;
                            if(texY>=s.tex.getHeight()) texY=s.tex.getHeight()-1;
                            int color=s.tex.getRGB(texX,texY);
                            if((color&0x00FFFFFF)!=0) pixels[y*screenWidth+stripe]=color;
                        }
                    }
                }
            }
        }

        repaint();
    }
//csgo movement
    void updateMovement(){
        if(inBattle) return;
        double moveSpeed=0.05, rotSpeed=0.03;
        if(keys[KeyEvent.VK_W]){
            if(worldMap[(int)(posX+dirX*moveSpeed)][(int)posY]==0) posX+=dirX*moveSpeed;
            if(worldMap[(int)posX][(int)(posY+dirY*moveSpeed)]==0) posY+=dirY*moveSpeed;
        }
        if(keys[KeyEvent.VK_S]){
            if(worldMap[(int)(posX-dirX*moveSpeed)][(int)posY]==0) posX-=dirX*moveSpeed;
            if(worldMap[(int)posX][(int)(posY-dirY*moveSpeed)]==0) posY-=dirY*moveSpeed;
        }
        if(keys[KeyEvent.VK_D]){
            double oldDirX=dirX;
            dirX=dirX*Math.cos(-rotSpeed)-dirY*Math.sin(-rotSpeed);
            dirY=oldDirX*Math.sin(-rotSpeed)+dirY*Math.cos(-rotSpeed);
            double oldPlaneX=planeX;
            planeX=planeX*Math.cos(-rotSpeed)-planeY*Math.sin(-rotSpeed);
            planeY=oldPlaneX*Math.sin(-rotSpeed)+planeY*Math.cos(-rotSpeed);
        }
        if(keys[KeyEvent.VK_A]){
            double oldDirX=dirX;
            dirX=dirX*Math.cos(rotSpeed)-dirY*Math.sin(rotSpeed);
            dirY=oldDirX*Math.sin(rotSpeed)+dirY*Math.cos(rotSpeed);
            double oldPlaneX=planeX;
            planeX=planeX*Math.cos(rotSpeed)-planeY*Math.sin(rotSpeed);
            planeY=oldPlaneX*Math.sin(rotSpeed)+planeY*Math.cos(rotSpeed);
        }
    }
//combat system BITTE ÄNDERT DAS ENDLICH
    void playerTurn(Move move){
        enemyHP -= move.damage;
        playerHP += move.heal;
        if(playerHP>20) playerHP=20;

        if(move.damage>0) playEffect("/attack.wav");
        if(move.heal>0) playEffect("/heal.wav");
        if(move.damage==8) playEffect("/crossbow.wav");


        if(enemyHP<=0){
            enemies.remove(battleEnemy);
            inBattle=false;
            battleEnemy=null;
            enemiesDefeated++;
            playerHP = playerHP+5;
            playerLevel += 1;

            // Unlock new moves after defeating enemies (pls nah)
            if(enemiesDefeated==1) unlockedMoves.add(new Move("Crossbow!", 8, 0));
            if(enemiesDefeated==1) unlockedMoves.add(new Move("absorb!", 1, 4));
            if(enemiesDefeated==3) unlockedMoves.add(new Move("Mega Strike!", 12, 0));
            if(enemiesDefeated==5) unlockedMoves.add(new Move("Full Heal!", 0, 20));
            if(enemiesDefeated==10) unlockedMoves.add(new Move("suicidal charge!", 30, -10));
            if(enemiesDefeated==20) unlockedMoves.add(new Move("flame engulf!", 30, -3));
            if(enemiesDefeated==30) unlockedMoves.add(new Move("Green tornado!", 10, 20));
            if(enemiesDefeated==40) unlockedMoves.add(new Move("Greatsword!", 25, 0));
            if(enemiesDefeated==50) unlockedMoves.add(new Move("Bow!", 20, 1));
            if(enemiesDefeated==60) unlockedMoves.add(new Move("Sneak attack!", 30, 0));
            if(enemiesDefeated==70) unlockedMoves.add(new Move("Rock punch!", 30, 0));
            if(enemiesDefeated==80) unlockedMoves.add(new Move("Electric shock!", 50, -10));
            if(enemiesDefeated==90) unlockedMoves.add(new Move("Mega absorb!", 10, 40));
            if(enemiesDefeated==100) unlockedMoves.add(new Move("EXTERMINATE BEAM", 100, 0));
        } else {
            // Enemy turn(no touchy)
            playerHP -= battleEnemy.damage;
            if(playerHP<=0){
                JOptionPane.showMessageDialog(this,"You died");
                System.exit(0);
            }
        }
    }
//controls garbage
    @Override public void keyPressed(KeyEvent e){
        keys[e.getKeyCode()]=true;

        if(inBattle){
            int key = e.getKeyCode();
            if(key>=KeyEvent.VK_1 && key<=KeyEvent.VK_9){
                int idx = key - KeyEvent.VK_1;
                if(idx < unlockedMoves.size()){
                    playerTurn(unlockedMoves.get(idx));
                }
            }
        }
    }
    @Override public void keyReleased(KeyEvent e){ keys[e.getKeyCode()]=false; }
    @Override public void keyTyped(KeyEvent e){}
//runtiem garbage
    @Override
    public void run(){
        while(true){
            updateMovement();
            render();
            try{Thread.sleep(19);}catch(Exception ignored){}
        }
    }
//start shi >_<
    public static void main(String[] args){
        JFrame frame=new JFrame("Dungeonexplorerer3D");
        Raycaster rc=new Raycaster();
        frame.add(rc);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        rc.requestFocusInWindow();
        new Thread(rc).start();
    }
}
