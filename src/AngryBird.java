import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;


public class AngryBird {
    static JFrame frm = new JFrame("憤怒鳥");
    static int ballX = 120, ballY = 550; // 小鳥的初始位置
    static int enemyX = 1000, enemyY = 565; // 敵人的初始位置
    static int woodX = 900, woodY = 550, woodW = 30, woodH = 80;
    static Enemy enemy = new Enemy(enemyX, enemyY); // 初始化敵人
    static WoodenBlock wood = new WoodenBlock(woodX, woodY, woodW, woodH);
    static ArrayList<WoodenBlock> woodenBlocks = new ArrayList<>(); // 木板列表

    public static Enemy getEnemy() {
        return enemy;
    }

    public static void setEnemy(Enemy enemy) {
        AngryBird.enemy = enemy;
    }
    WoodBlock[] woodBlocks = null;

    static int lastBallX, lastBallY; // 上一幀小鳥的位置
    static int birdSpeedX, birdSpeedY; // 小鳥的速度

    static float vx = 0, vy = 0; // 水平與垂直速度
    static Timer timer; // 計時器

    public static final double GRAVITY = 9.8; // 單位 m/s^2

    static JPanel pne = new JPanel() {
        Image bgImage = new ImageIcon("src/img/BG.jpg").getImage();
        Image ballImage = new ImageIcon("src/img/RedBird.png").getImage();
        Image shotImage = new ImageIcon("src/img/SlingShot.png").getImage();
        int offsetX, offsetY; // 滑鼠拖曳偏移量
        boolean dragging = false;
        List<Point> trajectory = new ArrayList<>(); //預測路徑

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
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (dragging) {
                        dragging = false;

                        // 計算釋放時水平方向與垂直方向的速度，放大水平方向速度
                        vx = (float) (e.getX() - offsetX - 120) ;  // 增大速度計算的放大倍數
                        vy = (float) (e.getY() - offsetY - 550) ;  // 放大垂直方向速度

                        // 記錄速度計算的日誌
                        System.out.println("Calculated vx: " + vx + ", vy: " + vy);
                        System.out.println("Calculated e.getX(): " + e.getX() + ", offsetX: " + offsetX+ "ballX " + ballX);
                        // 在放開滑鼠後開始計時器
                        timer = new Timer(16, evt -> moveBall());
                        timer.start();

                        // 清除預測路徑
                        trajectory.clear();
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragging) {
                        ballX = Math.max(0, Math.min(e.getX() - offsetX, getWidth() - 32));
                        ballY = Math.max(0, Math.min(e.getY() - offsetY, getHeight() - 32));

                        // 計算當前速度
                        float tempVx = (float) (ballX - 120);
                        float tempVy = (float) (ballY - 550);

                        // 清空路徑
                        trajectory.clear();

                        // 預測未來路徑
                        float posX = ballX, posY = ballY;
                        for (int i = 0; i < 100; i++) { // 計算 100 個點
                            posX -= tempVx;
                            posY -= tempVy;
                            tempVx *= 0.94; // 模擬摩擦
                            tempVy *= 0.9;
                            tempVy += -1; // 重力影響

                            // 超出邊界停止
                            if (posY >= getHeight() - 130) break;

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
                }
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

            // 繪製敵人（豬）
            enemy.draw(g, enemy.x, enemy.y);
            
            // wood.draw(g, scaleX, scaleY);
            // 繪製所有木板
            for (WoodenBlock block : woodenBlocks) {
                block.draw(g, wood.x, wood.y);
            }

            // 繪製小鳥
            int scaledBallX = (int) (ballX );
            int scaledBallY = (int) (ballY );
            g.drawImage(ballImage, scaledBallX, scaledBallY, 32, 32, this);
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
            if (timer != null) timer.stop();
            pne.repaint();
        });

        frm.add(pne);
        frm.setSize(1500, 750);
        frm.setVisible(true);
        frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    static void moveBall() {
        // 更新小鳥的位置
        ballX -= vx;
        ballY -= vy;
    
        // 減少水平方向和垂直方向的摩擦力，並控制重力影響
        vx *= 0.94;  // 減小摩擦，讓小鳥有更長的運動時間
        vy *= 0.9;  // 減小摩擦力
        vy += -1;     // 增加重力影響，調整重力使其更自然

        if (ballY >= pne.getHeight() - 130) {
            ballY = pne.getHeight() - 130; // 修正小鳥位置
            vy = 0; // 停止垂直速度
        }
    
        // 重新繪製界面
        pne.repaint();
    }

    public static void initWoodBlocks() {
        woodenBlocks.add(new WoodenBlock(950, 530, 30, 80));
        woodenBlocks.add(new WoodenBlock(1050, 530, 30, 80));
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
       
        Timer timer = new Timer(120, e -> { // 設定150毫秒後觸發
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
    
        // 防止超出地板（假設地板y=550）
        if (y > 570) {
            y = 570;
            vy = 0; // 停止垂直運動
            vx = 0; // 停止水平運動
        }
    }
}

class pig extends Enemy{
    public pig (int x, int y){
        super(x, y);
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
        this.rotationSpeed = 0.1; // 降低旋轉速度
        this.woodImage = new ImageIcon("src/img/wood.png").getImage();
        this.isFallingDown = false;
    }

    public void updatePosition() {
        if (!isStanding) {
            // 更新位置
            vx += ax;
            vy += ay;
            x += vx;
            y += vy;

            // 更新旋轉角度
            double targetAngle = isFallingDown ? Math.PI / 2 : -Math.PI / 2;
            if (Math.abs(rotationAngle - targetAngle) > 0.01) {
                // 使用更平滑的旋轉
                double angleDistance = targetAngle - rotationAngle;
                rotationAngle += angleDistance * rotationSpeed;
            }

            // 地面碰撞檢測
            if (y > 550) {
                y = 550;
                vy = 0;
                vx = 0;
                
                // 當木板停止時，設置為水平狀態
                rotationAngle = Math.PI / 2; // 保持水平
                isStanding = true;
            }
        }
    }

    public boolean checkCollision(Rectangle birdRect) {
        if (!isStanding) return false; // 如果木板已經倒下，不再檢測碰撞
        
        Rectangle blockRect = new Rectangle(x, y, width, height);
        return blockRect.intersects(birdRect);
    }

    public void hit(int birdSpeedX, int birdSpeedY, Rectangle birdRect) {
        isStanding = false;
        // 根據碰撞點決定旋轉方向
        double hitPoint = birdRect.getCenterY();
        double blockCenter = y + height / 2.0;
        
        isFallingDown = hitPoint > blockCenter;
        
        // 調整初始速度
        this.vx = birdSpeedX / 4; // 降低水平速度
        this.vy = birdSpeedY / 4; // 降低垂直速度
        
        // 根據碰撞位置設定初始旋轉角度
        rotationAngle = 0; // 從垂直狀態開始旋轉
    }

    public void draw(Graphics g, double scaleX, double scaleY) {
        Graphics2D g2d = (Graphics2D) g;
        // int scaledX = (int) (x * scaleX);
        // int scaledY = (int) (y * scaleY);

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
        this.isFallingDown = false;
    }
}