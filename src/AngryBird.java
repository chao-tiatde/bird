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
    static int enemyState = 0; // 0: 正常, 1: 煙霧, 2: 消失
    static JPanel pne = new JPanel() {
        Image bgImage = new ImageIcon("src/img/BG.jpg").getImage();
        Image ballImage = new ImageIcon("src/img/RedBird.png").getImage();
        Image enemyImage = new ImageIcon("src/img/pig.png").getImage();
        Image smokeImage = new ImageIcon("src/img/cloud1.png").getImage(); // 煙霧圖片
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
            if (enemyState == 0) { // 敵人處於正常狀態時檢測碰撞
                Rectangle birdRect = new Rectangle(ballX, ballY, 32, 32);
                Rectangle enemyRect = new Rectangle(enemyX, enemyY, 32, 32);

                if (birdRect.intersects(enemyRect)) {
                    enemyState = 1; // 切換到煙霧狀態
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            enemyState = 2; // 煙霧狀態後設為消失
                            repaint();
                        }
                    }, 150); // 150 毫秒後切換狀態
                }
            }

            // 增加木塊碰撞檢測
            Rectangle birdRect = new Rectangle(ballX, ballY, 32, 32);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);

            double scaleX = (double) getWidth() / 2560;
            double scaleY = (double) getHeight() / 1440;

            int scaledEnemyX = (int) (enemyX * scaleX);
            int scaledEnemyY = (int) (enemyY * scaleY);

            int scaledBallX = (int) (ballX * scaleX);
            int scaledBallY = (int) (ballY * scaleY);

            // 根據敵人狀態繪製
            if (enemyState == 0) {
                g.drawImage(enemyImage, scaledEnemyX, scaledEnemyY, 32, 32, this); // 正常狀態
            } else if (enemyState == 1) {
                g.drawImage(smokeImage, scaledEnemyX, scaledEnemyY, 32, 32, this); // 煙霧狀態
            }

            g.drawImage(ballImage, scaledBallX, scaledBallY, 32, 32, this); // 繪製小鳥

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
            enemyX = 1800;
            enemyY = 1160;
            enemyState = 0; // 重置敵人狀態為正常
            pne.repaint();
        });

        frm.add(pne);
        frm.setSize(800, 600);
        frm.setVisible(true);
        frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    
}