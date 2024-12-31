import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.*;
import javax.swing.*;


public class AngryBird {
    static JFrame frm = new JFrame("憤怒鳥");
    static int ballX = 120, ballY = 550; // 小鳥的初始位置
    static int enemyX = 1000, enemyY = 565; // 敵人的初始位置
    static int woodX = 900, woodY = 550, woodW = 30, woodH = 80;
    static Enemy enemy = new Enemy(enemyX, enemyY); // 初始化敵人
    static WoodenBlock wood = new WoodenBlock(woodX, woodY, woodW, woodH);
    static ArrayList<WoodenBlock> woodenBlocks = new ArrayList<>(); // 木板列表

    static int birdType = 1; // 1: 紅鳥, 2: 藍鳥, 3: 黃鳥
    static boolean blueBirdSplit = false; // 藍色鳥是否已分裂
    static boolean hideOriginalBlueBird = false; // 是否隱藏原始藍色鳥

    static class SplitBlueBird {
        int x, y;
        float vx, vy;

        SplitBlueBird(int x, int y, float vx, float vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }

        void update() {
            x -= vx;
            y -= vy;
            vx *= 0.94;
            vy *= 0.9;
            vy += -1; // 重力加速度

            // 確保鳥到達地面後停止墜落
            if (y >= pne.getHeight() - 123) {
                y = pne.getHeight() - 123;  // 限制到地面
                vy = 0;  // 停止垂直速度，防止再墜落
            }
        }
    }

    static List<SplitBlueBird> splitBirds = new ArrayList<>();

    public static Enemy getEnemy() {
        return enemy;
    }

    public static void setEnemy(Enemy enemy) {
        AngryBird.enemy = enemy;
    }

    public static class MusicPlayer {
        private Clip clip;
        
        public void play(String filePath) {
            try {
                File musicFile = new File(filePath);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(musicFile);
                clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
            }
        }
        
        public void stop() {
            if (clip != null) {
                clip.stop();
                clip.close();
            }
        }
        
        public void pause() {
            if (clip != null) {
                clip.stop();
            }
        }
        
        public void resume() {
            if (clip != null) {
                clip.start();
            }
        }
    }

    static int lastBallX, lastBallY; // 上一幀小鳥的位置
    static int birdSpeedX, birdSpeedY; // 小鳥的速度

    static float vx = 0, vy = 0; // 水平與垂直速度
    static Timer timer; // 計時器

    public static final double GRAVITY = 9.8; // 單位 m/s^2

    static JPanel pne = new JPanel() {
        Image bgImage = new ImageIcon("src/img/BG.jpg").getImage();
        Image redBirdImage = new ImageIcon("src/img/RedBird.png").getImage();
        Image blueBirdImage = new ImageIcon("src/img/BlueBird.png").getImage();
        Image yellowBirdImage = new ImageIcon("src/img/YellowBird.png").getImage();
        Image shotImage = new ImageIcon("src/img/SlingShot.png").getImage();
        int offsetX, offsetY; // 滑鼠拖曳偏移量
        boolean dragging = false;
        List<Point> trajectory = new ArrayList<>(); //預測路徑

        AngryBird.MusicPlayer flySound = new AngryBird.MusicPlayer();        
        MusicPlayer drag = new MusicPlayer();

        {
            // 滑鼠事件
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // 判斷是否點擊在小鳥內部
                    if (e.getX() >= ballX && e.getX() <= ballX + 32 &&
                        e.getY() >= ballY && e.getY() <= ballY + 32) {
                        dragging = true;
                        offsetX = e.getX() - ballX;
                        offsetY = e.getY() - ballY;
                        if (timer != null) timer.stop();
                        
                        drag.play("src/music/slingshot streched.wav");
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (dragging) {      
                        if (birdType == 1){
                            flySound .play("src/music/redbird_yell01.wav");
                        } else if (birdType == 2){
                            flySound .play("src/music/bird 02 flying.wav");
                        } else if (birdType == 3){
                            flySound .play("src/music/bird 05 flying.wav");
                        }               
                        
                        dragging = false;

                        // 計算釋放時水平方向與垂直方向的速度，放大水平方向速度
                        vx = (float) ((e.getX() - offsetX - 120) * 0.3);  // 增大速度計算的放大倍數
                        vy = (float) ((e.getY() - offsetY - 550) * 0.3);  // 放大垂直方向速度

                        // 記錄速度計算的日誌
                        System.out.println("Calculated vx: " + vx + ", vy: " + vy);
                        System.out.println("Calculated e.getX(): " + e.getX() + ", offsetX: " + offsetX+ "ballX " + ballX);
                        // 在放開滑鼠後開始計時器
                        timer = new Timer(10, _ -> {
                            moveBall();
                            checkCollision(); // 在每次更新小鳥位置後立即檢測碰撞
                        });
                        
                        timer.start();

                        // 清除預測路徑
                        trajectory.clear();
                        blueBirdSplit = false; // 重置藍色鳥的分裂狀態
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {       
                    drag.stop();             
                    if (birdType == 2 && !blueBirdSplit) {
                        blueBirdSplit = true;
                        hideOriginalBlueBird = true;
                        System.out.println("藍色鳥分裂!");

                        // 分裂成三個小鳥
                        splitBirds.add(new SplitBlueBird(ballX, ballY, vx * 0.8f, vy * 1.1f));
                        splitBirds.add(new SplitBlueBird(ballX, ballY, vx * 1.2f, vy * 0.9f));
                        splitBirds.add(new SplitBlueBird(ballX, ballY, vx, vy));
                        
                        AngryBird.MusicPlayer blueSound = new AngryBird.MusicPlayer();
                        
                        // flySound.stop();
                        // blueSound.play("src/music/bird next military a1.wav");
                    }

                    if (birdType == 3) {
                        vx -= 20; // 可以根據需求調整這個增量值
                        System.out.println("黃色鳥的水平速度增加: " + vx);
                    }
                }

                
            });

            addMouseMotionListener((MouseMotionListener) new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragging) {                        
                        ballX = Math.max(0, Math.min(e.getX() - offsetX, getWidth() - 32));
                        ballY = Math.max(0, Math.min(e.getY() - offsetY, getHeight() - 32));

                        // 計算當前速度
                        float tempVx = (float) ((ballX - 120) * 0.3);
                        float tempVy = (float) ((ballY - 550) * 0.3);

                        // 清空路徑
                        trajectory.clear();

                        // 預測未來路徑
                        int posX = (120 + offsetX);
                        int posY = (550 + offsetY);
                        for (int i = 0; i < 300; i++) { // 計算 100 個點
                            posX -= tempVx;
                            posY -= tempVy;
                            tempVx += ((-tempVx)*0.002);
                            tempVy += -0.5;

                            // 超出邊界停止
                            if ((posY >= getHeight() - 130 + 32) && posX >= (120 + offsetX)) break;
                            trajectory.add(new Point((int) posX, (int) posY));
                        }

                        // 更新速度
                        birdSpeedX = ballX - lastBallX;
                        birdSpeedY = ballY - lastBallY;

                        // 記錄上一次位置
                        lastBallX = ballX;
                        lastBallY = ballY;

                        // 碰撞檢測
                        checkCollision();

                        repaint(); // 重新繪製
                    }
                }
            });
        }
    
            
        // 檢測小鳥與木板和敵人是否碰撞
        private void checkCollision() {
            // 創建鳥的矩形，假設鳥的大小是 32x32
            Rectangle birdRect = new Rectangle(ballX, ballY, 32, 32);
            
            // 檢查每個木板
            for (WoodenBlock block : woodenBlocks) {
                if (block.checkCollision(birdRect)) {
                    // 傳遞鳥的速度和矩形，處理碰撞
                    block.hit(birdSpeedX, birdSpeedY, birdRect); 
                    System.out.println("木板被擊倒！");
                    flySound.stop();
                }
            }

            // 檢查木頭與豬的碰撞
            Rectangle blockRect = new Rectangle(wood.x, wood.y, wood.width, wood.height);
            Rectangle enemyRect = new Rectangle(enemy.x, enemy.y, 32, 32);
            if (blockRect.intersects(enemyRect)) {
                System.out.println("木頭碰到豬！");
                wood.y = enemy.y - wood.height; // 木頭疊在豬的上方
                wood.vy = 0; // 停止垂直速度
                wood.vx = 0; // 停止水平速度
            }
        
            // 可能還有其他碰撞檢查
            checkCollisionBird();
        }
        

        // 敵人被小鳥擊中
        private void checkCollisionBird() {
            Rectangle birdRect = new Rectangle(ballX, ballY, 32, 32);
            Rectangle enemyRect = new Rectangle(enemy.x, enemy.y, 32, 32);
        
            if (birdRect.intersects(enemyRect) && !enemy.isTimerRunning) {
                System.out.println("敵人被小鳥擊中！");
                enemy.isTimerRunning = true;
                enemy.vx = birdSpeedX / 2;
                enemy.vy = birdSpeedY / 2;
                enemy.setState(1); // 切換為煙霧狀態
                AngryBird.MusicPlayer pigSound = new AngryBird.MusicPlayer();
                pigSound .play("src/music/piglette collision a1.wav");
                enemy.updateState(); // 啟動煙霧倒計時
                pne.repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
            g.drawImage(shotImage, 100, 490, 100, 150, this);

            g.setColor(Color.ORANGE);
            for (int i = 0; i < trajectory.size() - 1; i++) {
                Point p1 = trajectory.get(i);
                Point p2 = trajectory.get(i + 1);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }

            // 更新敵人和木板的位置
            enemy.updatePosition();
            for (WoodenBlock block : woodenBlocks) {
                block.updatePosition();
            }

            // 繪製所有木板
            for (WoodenBlock block : woodenBlocks) {
                block.draw(g, wood.x, wood.y);
            }

            // 繪製敵人（豬）
            enemy.draw(g, enemy.x, enemy.y);

            if (birdType == 1) {
                g.drawImage(redBirdImage, ballX, ballY, 32, 32, this);
            } else if (birdType == 2) {
                if (!hideOriginalBlueBird) {
                    g.drawImage(blueBirdImage, ballX, ballY, 32, 32, this);
                }

                // 繪製分裂後的藍色鳥
                for (SplitBlueBird bird : splitBirds) {
                    bird.update();  // 更新鳥的位置
                    g.drawImage(blueBirdImage, bird.x, bird.y, 25, 25, this); // 繪製藍色小鳥
                }
            } else if (birdType == 3) {
                // 繪製黃色鳥
                g.drawImage(yellowBirdImage, ballX - 35, ballY - 15, 110, 64, this);
            }
        }
    };

    static JButton btn = new JButton("重置");
    static JButton btnBird = new JButton("切換憤怒鳥");

    public static void main(String[] args) {
        pne.setLayout(null);
        btn.setBounds(10, 10, 80, 30); // 按鈕位置
        btnBird.setBounds(100, 10, 100, 30);
        pne.add(btn);
        pne.add(btnBird);

        initWoodBlocks();

        MusicPlayer player = new MusicPlayer();
        player.play("src/music/Angry Birds Theme.wav");  // 替換為您的音樂文件路徑

        // 重置按鈕功能
        
        btn.addActionListener(_ -> {
            ballX = 120;
            ballY = 550;
            enemy.reset(); // 重置敵人
            for (WoodenBlock block : woodenBlocks) {
                block.reset(); // 重置所有木板
            }
            vx = 0;
            vy = 0;
            hideOriginalBlueBird = false;
            splitBirds.clear();  // 清空分裂鳥列表
            if (timer != null) timer.stop();
            pne.repaint();
        });

        btnBird.addActionListener(e -> {
            birdType = (birdType % 3) + 1; // 在三種鳥之間切換
            ballX = 120;
            ballY = 550;
            vx = 0;
            vy = 0;
            hideOriginalBlueBird = false;
            splitBirds.clear();  // 清空分裂鳥列表
            if (timer != null) timer.stop();
            System.out.println("切換到" + (birdType == 1 ? "紅色鳥" : birdType == 2 ? "藍色鳥" : "黃色鳥"));
            pne.repaint();
        });

        frm.add(pne);
        frm.setSize(1500, 750);
        frm.setResizable(false);
        frm.setVisible(true);
        frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    static void moveBall() {
        // 更新小鳥的位置
        //避免彈弓拉到地平面下觸發落地vy=0
        if (ballX < 120 && ballY > 550) {
            ballX -= (vx / 0.3);
            ballY -= (vy / 0.3);
        } else{
            ballX -= vx;
            ballY -= vy;      
            
            if (ballY >= pne.getHeight() - 130) {
                ballY = pne.getHeight() - 130; // 修正小鳥位置
                vy = 0; // 停止垂直速度
                if (vx < 0) {
                    vx += 0.7;    
                } else if (vx > 0) {
                    vx = 0;
                }
            } else {
                vx += ((-vx)*0.002); //空氣阻力
                vy += -0.5; //重力
            }
        }

        if (Math.abs(vx) < 0.1) vx = 0; // 防止速度過小抖動
        if (Math.abs(vy) < 0.1) vy = 0;


        // 減少水平方向和垂直方向的摩擦力，並控制重力影響
        // vx *= 0.94;  // 減小摩擦，讓小鳥有更長的運動時間
        // vy *= 0.9;  // 減小摩擦力
        // vy += -1;     // 增加重力影響，調整重力使其更自然
    
        // 重新繪製界面
        pne.repaint();
    }

    public static void initWoodBlocks() {
        woodenBlocks.add(new WoodenBlock(950, 530, 30, 80));
        woodenBlocks.add(new WoodenBlock(1050, 530, 30, 80));
        /* WoodenBlock block = new WoodenBlock(1000, 485, 30, 100);
        block.rotationAngle = Math.PI / 2; // 初始傾斜 90 度
        block.initialRotationAngle = Math.PI / 2; // 記錄初始角度
        woodenBlocks.add(block); */
    }
}

// 豬的類別
class Enemy {
    int x, y;
    int vx, vy; // 加入水平和垂直速度
    int ax, ay; // 加入水平和垂直加速度
    int state; // 0: 正常, 1: 煙霧, 2: 消失
    int displayWidth;
    int displayHeight;
    Image enemyImage, smokeImage;

    private Timer smokeTimer;
    boolean isTimerRunning = false; // 計時器是否正在執行

    public Enemy(int x, int y) {
        this.x = x;
        this.y = y;
        this.vx = 0; // 初始速度
        this.vy = 0;
        this.ax = 0; // 初始加速度
        this.ay = 10; // 垂直加速度，模擬重力
        this.state = 0; // 初始狀態為正常
        this.enemyImage = new ImageIcon("src/img/pig.png").getImage();
        this.smokeImage = new ImageIcon("src/img/cloud1.png").getImage();
    }

   // 更新敵人狀態（如煙霧效果）
   public void updateState() {
    // 如果敵人狀態是煙霧且計時器尚未運行
    if (state == 1) {
        isTimerRunning = true; // 防止多次運行
        Timer timer = new Timer(100, e -> { // 設定150毫秒後觸發
            state = 2; // 切換到消失狀態
          
            System.out.println("敵人變成消失狀態！");
        });
        timer.setRepeats(false); // 設置計時器只觸發一次
        timer.start(); // 啟動計時器
    }
}

    // 繪製敵人
    public void draw(Graphics g, double scaleX, double scaleY) {
    
        switch (state) {
            case 0:  // 正常狀態
                g.drawImage(enemyImage, x, y, 32, 32, null);
                break;
            case 1:  // 煙霧狀態
                g.drawImage(smokeImage, x, y, 32, 32, null);
                break;
            case 2:  // 消失狀態
                // 不繪製任何東西
                break;
        }
    }    

    // 設置豬的狀態
    public void setState(int state) {
        this.state = state;
    }

    // 重置豬的狀態
    public void reset() {
        if (smokeTimer != null) {
            smokeTimer.stop();  // 停止正在執行的計時器
        }
        state = 0;
        x = 1000;
        y = 565;
        vx = 0;
        vy = 0;
        ax = 0;
        ay = 10;
        isTimerRunning = false;
    }
    

    public void updatePosition() {
        vx += ax; // 更新水平速度
        vy += ay; // 更新垂直速度
        x += vx; // 更新水平位置
        y += vy; // 更新垂直位置
    
        // 防止超出地板（假設地板y=570）
        if (y > 570) {
            y = 570;
            vy = 0; // 停止垂直運動
            vx = 0; // 停止水平運動
        }
    }
}

class WoodenBlock {
    int x, y;
    int vx, vy;
    int ax, ay;
    int width, height;
    int initialX, initialY;
    boolean isStanding;
    Image woodImage;
    double rotationAngle;
    double rotationSpeed;
    boolean isFallingDown; // 用來記錄木板是否向下倒
    double initialRotationAngle; // 新增變量保存初始角度
    private boolean hasCollided = false; // 新增變數追蹤是否已發生碰撞

    public WoodenBlock(int x, int y, int width, int height) {
        this.initialX = x;
        this.initialY = y;
        this.x = x;
        this.y = y;
        this.vx = 0;
        this.vy = 0;
        this.ax = 0;
        this.ay = 10;
        this.width = width;
        this.height = height;
        this.isStanding = true;
        this.rotationAngle = 0;
        this.rotationSpeed = 0.05; // 降低旋轉速度
        this.initialRotationAngle = 0; // 默認初始角度為 0
        this.woodImage = new ImageIcon("src/img/wood.png").getImage();
        this.isFallingDown = false;
    }

    public void updatePosition() {
        if (!isStanding) {
            // 更新速度
            vx += ax;
            vy += ay;
            x += vx;
            y += vy;
    
            // 根據旋轉方向設置目標角度
            double targetAngle = isFallingDown ? Math.PI / 2 : -Math.PI / 2;
    
            // 平滑旋轉
            // double angleDistance = targetAngle - rotationAngle;
            // if (Math.abs(angleDistance) > 0.01) {
            //     rotationAngle += angleDistance * rotationSpeed;
            // }
            double torque = vx * 0.01; // 模擬扭矩效果，與速度相關
            rotationAngle += torque;
    
            // 防止超出地板
            if (y != 550) {
                y = 550;
                vy = 0;
                vx = 0;
                rotationAngle = targetAngle; // 停止旋轉
                isStanding = true;
            }
        }
    }

    public boolean checkCollision(Rectangle birdRect) {
        if (hasCollided || !isStanding) return false; // 如果已碰撞或已倒下，不再檢測
        
        Rectangle blockRect = new Rectangle(x, y, width, height);
        return blockRect.intersects(birdRect);
    }

    public void hit(int birdSpeedX, int birdSpeedY, Rectangle birdRect) {
        if (hasCollided) return;
        hasCollided = true;
        isStanding = false;
        
        // 根據碰撞點決定旋轉方向
        double hitPoint = birdRect.getCenterY();
        double blockCenter = y + height / 2.0;
    
        isFallingDown = hitPoint > blockCenter;
    
        // 根據速度設置初始旋轉速度
        rotationSpeed = Math.abs(birdSpeedX + birdSpeedY) * 0.005;
        rotationAngle = 0; // 從無旋轉開始
        vx = Math.max(Math.min(birdSpeedX / 4, 5), -5); // 限制水平速度
        vy = Math.max(Math.min(birdSpeedY / 4, 5), -5); // 限制垂直速度 

        AngryBird.MusicPlayer hitSound = new AngryBird.MusicPlayer();
        hitSound .play("src/music/wood collision a1.wav");
    }
    

    public void draw(Graphics g, double scaleX, double scaleY) {
        Graphics2D g2d = (Graphics2D) g;

        // 保存當前變換
        AffineTransform oldTransform = g2d.getTransform();

        // 設置旋轉中心點
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        
        // 應用旋轉
        g2d.rotate(rotationAngle, centerX, centerY);
        g.drawImage(woodImage, x, y, width, height, null);
        
        // 恢復原來的變換
        g2d.setTransform(oldTransform);
    }

    public void reset() {
        this.isStanding = true;
        this.x = initialX;
        this.y = initialY;
        this.rotationAngle = 0;
        this.vx = 0;
        this.vy = 0;
        this.rotationAngle = initialRotationAngle; // 恢復初始角度
        this.isFallingDown = false;
        this.hasCollided = false; // 重置碰撞狀態
    }
}

// class Ground {
//     // int x = 0, y = AngryBird.pne.getHeight() - 130;
//     // int width = AngryBird.pne.getWidth(), height = 300;
//     // int initialX, initialY;
//     Rectangle groundRect;
//     boolean reverse = false;

//     public Ground(int x, int y, int w, int h) {
//         groundRect = new Rectangle(x, y, w, h);
//     }

//     public boolean check(Rectangle birdRect) {
        
//         //鳥鳥有沒有碰到地板
//         if (groundRect.intersects(birdRect)) {
//             if (Math.abs(AngryBird.vy) < 4){
//                 AngryBird.vy = 0; //y軸速度太小就不彈跳了
//             } else{
//                 //速度夠就反彈
//                 AngryBird.ballY = (groundRect.y -32);
//                 AngryBird.vy += AngryBird.vy * -1.2;
//             }

//             //撞到地板x軸速度也要減弱一點
//             if (AngryBird.vx < 0) {
//                 if(Math.abs(AngryBird.vx) < 2) AngryBird.vx = 0;
//                 else AngryBird.vx += 1;
//             } else if (AngryBird.vx > 0) {
//                 if(Math.abs(AngryBird.vx) < 2) AngryBird.vx = 0;
//                 else AngryBird.vx += -1;
//             }
//         }
//         return groundRect.intersects(birdRect);
//     }
    
// }