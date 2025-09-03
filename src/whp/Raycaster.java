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
    final int screenWidth = 640;
    final int screenHeight = 480;

    final int mapWidth = 24;
    final int mapHeight = 24;

    int[][] worldMap = {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,1,0,2,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
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
        BufferedImage tex;
        Sprite(double x, double y, BufferedImage tex, int health, int damage){
            this.x = x; this.y = y; this.tex = tex; this.health = health; this.damage = damage;
        }
    }

    boolean inBattle=false; Sprite battleEnemy; int playerHP=20, enemyHP; int playerLevel=1; int enemiesDefeated=0;
    double shakeX=0, shakeY=0;



    java.util.List<Sprite> enemies = new ArrayList<>();

    BufferedImage enemyTex;

    // Define moves
    static class Move {
        String name;
        int damage;
        int heal;
        Move(String n, int d, int h) { name = n; damage = d; heal = h; }
    }
    Clip bgClip;
    Clip battleClip;

    java.util.List<Move> unlockedMoves = new ArrayList<>();

    public Raycaster() {
        screen = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) screen.getRaster().getDataBuffer()).getData();
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setFocusable(true);
        addKeyListener(this);

        try {
            enemyTex = ImageIO.read(getClass().getResource("/sprite2.png"));
        } catch (IOException e) { e.printStackTrace(); }
        playBackground("/bg.wav");


        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                if (x >= 0 && x < worldMap.length && y >= 0 && y < worldMap[0].length) {
                    if (worldMap[x][y] >= 2 && worldMap[x][y] <= 4) {
                        enemies.add(new Sprite(x + 0.5, y + 0.5, enemyTex, 10,5));
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


    @Override
    public void paintComponent(Graphics g){
        g.drawImage(screen,0,0,null);
        if(inBattle){
            g.setColor(Color.RED);
            g.drawString("BATTLE!", 300, 80);
            g.setColor(Color.WHITE);
            g.drawString("Player HP: " + playerHP, 70, 120);
            g.drawString("Enemy HP: " + enemyHP, 400, 120);
            g.drawString("Choose a move:", 150, 160);

            // Show unlocked moves
            for (int i = 0; i < unlockedMoves.size(); i++) {
                Move m = unlockedMoves.get(i);
                g.drawString((i+1) + ". " + m.name, 180, 190 + i*20);
            }
        }
    }

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

// Update enemies with line-of-sight
            for(Sprite s: enemies){
                double dx = posX - s.x;
                double dy = posY - s.y;
                double dist = Math.sqrt(dx*dx + dy*dy);

                // Check line of sight using simple step along the line
                boolean canSeePlayer = false;
                int steps = (int)(dist * 10); // more steps for accuracy
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
                    // check collisions before moving
                    if(worldMap[(int)(s.x+moveX)][(int)s.y] == 0) s.x += moveX;
                    if(worldMap[(int)s.x][(int)(s.y+moveY)] == 0) s.y += moveY;
                } else if(dist <= 0.5){
                    // trigger battle
                    inBattle = true;
                    battleEnemy = s;
                    enemyHP = s.health;
                    playBattle("/battle.wav");
                }
            }


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

    void playerTurn(Move move){
        enemyHP -= move.damage;
        playerHP += move.heal;
        if(playerHP>20) playerHP=20;

        if(move.damage>0) playEffect("/attack.wav");
        if(move.heal>0) playEffect("/heal.wav");
        if(move.heal>0) playEffect("/heal.wav");


        if(enemyHP<=0){
            enemies.remove(battleEnemy);
            inBattle=false;
            battleEnemy=null;
            enemiesDefeated++;

            // Unlock new moves after defeating enemies
            if(enemiesDefeated==1) unlockedMoves.add(new Move("Strong Attack", 8, 0));
            if(enemiesDefeated==3) unlockedMoves.add(new Move("Mega Strike", 12, 0));
            if(enemiesDefeated==5) unlockedMoves.add(new Move("Full Heal", 0, 20));
            if(enemiesDefeated==10) unlockedMoves.add(new Move("suicidal charge", 30, -10));
        } else {
            // Enemy turn
            playerHP -= 6;
            if(playerHP<=0){
                JOptionPane.showMessageDialog(this,"You fainted! Game over!");
                System.exit(0);
            }
        }
    }

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

    @Override
    public void run(){
        while(true){
            updateMovement();
            render();
            try{Thread.sleep(16);}catch(Exception ignored){}
        }
    }

    public static void main(String[] args){
        JFrame frame=new JFrame("Dungeonexplorerer");
        Raycaster rc=new Raycaster();
        frame.add(rc);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        rc.requestFocusInWindow();
        new Thread(rc).start();
    }
}
