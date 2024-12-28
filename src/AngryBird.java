import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class AngryBird {
    static JFrame frm = new JFrame("憤怒鳥");
    static int ballX = 120, ballY = 1160; // 小鳥的初始位置
    static int enemyX = 1800, enemyY = 1160; // 敵人的初始位置
    static Enemy enemy = new Enemy(enemyX, enemyY); // 初始化敵人
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
                        ballY = Math.max(0, Math.min(ballY, 1440 - 32));

                        // 碰撞檢測
                        checkCollision();

                        repaint(); // 重新繪製
                    }
                }
            });
        }

        // 檢測小鳥與敵人是否碰撞
        private void checkCollision() {
            Rectangle birdRect = new Rectangle(ballX, ballY, 32, 32);
            Rectangle enemyRect = new Rectangle(enemy.x, enemy.y, 32, 32);

            if (birdRect.intersects(enemyRect)) {
                enemy.setState(1); // 豬被擊中，切換到煙霧狀態
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
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

        // 重置按鈕功能
        btn.addActionListener(e -> {
            ballX = 120;
            ballY = 1160;
            enemy.reset(); // 重置敵人
            pne.repaint();
        });

        frm.add(pne);
        frm.setSize(800, 600);
        frm.setVisible(true);
        frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
