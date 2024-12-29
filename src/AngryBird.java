import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;



public class AngryBird extends JFrame implements Runnable{
    static JFrame frm = new JFrame("憤怒鳥");
    static int ballX = 120, ballY = 1160; // 小鳥的初始位置
    static int enemyX = 1800, enemyY = 1160; // 敵人的初始位置
    static int woodX = 1700, woodY = 1058, woodW = 30, woodH = 80;
    static Enemy enemy = new Enemy(enemyX, enemyY); // 初始化敵人
    static WoodenBlock wood = new WoodenBlock(woodX, woodY, woodW, woodH);
    static ArrayList<WoodenBlock> woodenBlocks = new ArrayList<>(); // 木板列表
    WoodBlock[] woodBlocks = null;

    static int lastBallX, lastBallY; // 上一幀小鳥的位置
    static int birdSpeedX, birdSpeedY; // 小鳥的速度

    private long timerClick;
    private Graphics graphics = null; //画笔
    private Bullet bullet = null;
    private  int t ;
    private int v0;
    private int vt;
    private int g;
    
    public AngryBird() {
        //画板的宽度
        int windowWidth = 629;
        //画板的高度
        int windowHeight = 990;
        setLayout(null);
        setSize(windowWidth, windowHeight);
        setLocationRelativeTo(null);
        setVisible(true);
        timerClick = 0;
        v0 = 100;
        g = 10;
        graphics = getContentPane().getGraphics();
        bullet = new Bullet(0, 600, 64, 64, "wood.png");
    }



    static JPanel pne = new JPanel() {
        Image bgImage = new ImageIcon("src/img/BG.jpg").getImage();
        Image ballImage = new ImageIcon("src/img/RedBird.png").getImage();
        int offsetX, offsetY; // 滑鼠拖曳偏移量
        boolean dragging = false;

        {
            // 滑鼠事件
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    double scaleX = (double) getWidth() / 2560;
                    double scaleY = (double) getHeight() / 1440;
                    int scaledBallX = (int) (ballX * scaleX);
                    int scaledBallY = (int) (ballY * scaleY);

                    // 判斷是否點擊在小鳥內部
                    if (e.getX() >= scaledBallX && e.getX() <= scaledBallX + 32 &&
                        e.getY() >= scaledBallY && e.getY() <= scaledBallY + 32) {
                        dragging = true;
                        offsetX = e.getX() - scaledBallX;
                        offsetY = e.getY() - scaledBallY;
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    dragging = false;
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragging) {
                        double scaleX = (double) getWidth() / 2560;
                        double scaleY = (double) getHeight() / 1440;

                        ballX = (int) ((e.getX() - offsetX) / scaleX);
                        ballY = (int) ((e.getY() - offsetY) / scaleY);

                        // 防止小鳥移出視窗邊界
                        ballX = Math.max(0, Math.min(ballX, 2560 - 32));
                        ballY = Math.max(0, Math.min(ballY, 1200 - 32));

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
            Rectangle birdRect = new Rectangle(ballX, ballY, 32, 32);

            // 遍歷木板
            // 檢測小鳥是否擊中木板
            for (WoodenBlock block : woodenBlocks) {
                if (block.checkCollision(birdRect)) {
                    double impactForce = Math.sqrt(birdSpeedX * birdSpeedX + birdSpeedY * birdSpeedY);
                    if (impactForce > 10) { // 假設 10 是木板倒下的閾值
                        block.hit(); // 木板倒下
                        System.out.println("木板被擊倒！");
                    }
                }
            }
            checkCollisionBird();
        }

        // 敵人被小鳥擊中
        private void checkCollisionBird() {
            Rectangle birdRect = new Rectangle(ballX, ballY, 32, 32);
            Rectangle enemyRect = new Rectangle(enemy.x, enemy.y, 32, 32);
    
            if (birdRect.intersects(enemyRect)) {
                enemy.setState(1); // 豬被擊中，切換到煙霧狀態
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("小鳥碰到敵人！");
                        enemy.setState(2); // 煙霧顯示一段時間後，切換為消失
                        repaint();
                    }
                }, 150); // 150毫秒後切換狀態
            }
        }


        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);

            double scaleX = (double) getWidth() / 2560;
            double scaleY = (double) getHeight() / 1440;

            // 繪製敵人（豬）
            enemy.draw(g, scaleX, scaleY);
            // wood.draw(g, scaleX, scaleY);
            // 繪製所有木板
            for (WoodenBlock block : woodenBlocks) {
                block.draw(g, scaleX, scaleY);
            }

            // 繪製小鳥
            int scaledBallX = (int) (ballX * scaleX);
            int scaledBallY = (int) (ballY * scaleY);
            g.drawImage(ballImage, scaledBallX, scaledBallY, 32, 32, this);
        }
    };

    static JButton btn = new JButton("重置");

    public static void main(String[] args) {
        pne.setLayout(null);
        btn.setBounds(10, 10, 80, 30); // 按鈕位置
        pne.add(btn);

        AngryBird angryBird = new AngryBird();
        Thread thread = new Thread(angryBird);
        thread.start();

        initWoodBlocks();

        // 重置按鈕功能
        btn.addActionListener(e -> {
            ballX = 120;
            ballY = 1160;
            enemy.reset(); // 重置敵人
            for (WoodenBlock block : woodenBlocks) {
                block.reset(); // 重置所有木板
            }
            pne.repaint();
        });

        frm.add(pne);
        frm.setSize(800, 600);
        frm.setVisible(true);
        frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void initWoodBlocks() {
        woodenBlocks.add(new WoodenBlock(1700, 1058, 30, 80));
        woodenBlocks.add(new WoodenBlock(1900, 1058, 30, 80));
    }

    @Override
    public void run() {
        while (true)
        {
            timerClick++;
            graphics.drawImage(bullet.getImage(), bullet.getX(), bullet.getY(),bullet.getDisplayWidth(),bullet.getDisplayHeight(),this);
 
            if (timerClick % 100 == 0)
            {
                t += 1;
                vt = v0 - g * t;
                int h = (int) (v0 * t -1 / 2.0 * g * t * t);
                bullet.setX(t * 10);
                bullet.setY(600 - h);
 
                System.out.println(bullet.getX() + " " + bullet.getY());
            }
 
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}

// 豬的類別
class Enemy {
    int x, y;
    int state; // 0: 正常, 1: 煙霧, 2: 消失
    Image enemyImage, smokeImage;

    public Enemy(int x, int y) {
        this.x = x;
        this.y = y;
        this.state = 0; // 初始狀態為正常
        this.enemyImage = new ImageIcon("src/img/pig.png").getImage();
        this.smokeImage = new ImageIcon("src/img/cloud1.png").getImage();
    }

    // 更新敵人狀態（如煙霧效果）
    public void updateState() {
        if (state == 1) {
            // 煙霧效果持續一段時間後消失
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    state = 2; // 設為消失
                }
            }, 150); // 煙霧效果顯示150毫秒
        }
    }

    // 繪製敵人
    public void draw(Graphics g, double scaleX, double scaleY) {
        int scaledX = (int) (x * scaleX);
        int scaledY = (int) (y * scaleY);

        if (state == 0) {
            g.drawImage(enemyImage, scaledX, scaledY, 32, 32, null); // 正常顯示
        } else if (state == 1) {
            g.drawImage(smokeImage, scaledX, scaledY, 32, 32, null); // 煙霧顯示
        }
    }

    // 設置豬的狀態
    public void setState(int state) {
        this.state = state;
    }

    // 重置豬的狀態
    public void reset() {
        this.state = 0;
        this.x = 1800;
        this.y = 1160;
    }
}

class WoodenBlock {
    int x, y; // 木板的位置
    int width, height; // 木板的寬度和高度
    int initialX, initialY; // 木板的初始位置
    boolean isStanding; // 木板是否還在立著
    Image woodImage;

    public WoodenBlock(int x, int y, int width, int height) {
        this.initialX = x; // 保存初始位置
        this.initialY = y;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.isStanding = true; // 初始狀態為立著
        this.woodImage = new ImageIcon("src/img/wood.png").getImage();
    }

    // 碰撞檢測
    public boolean checkCollision(Rectangle birdRect) {
        Rectangle blockRect = new Rectangle(x, y, width, height);
        return isStanding && blockRect.intersects(birdRect);
    }

    // 處理碰撞後的狀態
    public void hit() {
        isStanding = false; // 被擊中後，木板倒下
    }

    // 繪製木板
    public void draw(Graphics g, double scaleX, double scaleY) {
        Graphics2D g2d = (Graphics2D) g;
        int scaledX = (int) (x * scaleX);
        int scaledY = (int) (y * scaleY);

        if (isStanding) {
        g.drawImage(woodImage, scaledX, scaledY, width, height, null);
        } else {
            // 木板倒下時旋轉
            g2d.rotate(Math.toRadians(90), scaledX + width / 2.0, scaledY + height / 2.0);
            g.drawImage(woodImage, scaledX, scaledY, width, height, null);
            g2d.rotate(Math.toRadians(-90), scaledX + width / 2.0, scaledY + height / 2.0);
        }
    }

    // 重置木板的狀態
    public void reset() {
        this.isStanding = true; // 重置為立著
        this.x = initialX;
        this.y = initialY;
    }    
}

class Base
{
    private int x;
    private int y;
    private int displayWidth;
    private int displayHeight;
    private final Image image;
 
    public Base(int x, int y, int displayWidth, int displayHeight, String fileName) {
        this.x = x;
        this.y = y;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.image = Toolkit.getDefaultToolkit().getImage( "resources\\images\\" + fileName);
    }
 
    Base(Image image) {
        this.image = image;
    }
 
    public int getX() {
        return x;
    }
 
    public void setX(int x) {
        this.x = x;
    }
 
    public int getY() {
        return y;
    }
 
    public void setY(int y) {
        this.y = y;
    }
 
    public int getDisplayWidth() {
        return displayWidth;
    }
 
    public void setDisplayWidth(int displayWidth) {
        this.displayWidth = displayWidth;
    }
 
    public int getDisplayHeight() {
        return displayHeight;
    }
 
    public void setDisplayHeight(int displayHeight) {
        this.displayHeight = displayHeight;
    }
 
    public Image getImage() {
        return image;
    }
}
 
class Bullet extends Base
{
    public Bullet(int x, int y, int displayWidth, int displayHeight, String fileName)
    {
        super(x, y, displayWidth, displayHeight, fileName);
    }
}
 