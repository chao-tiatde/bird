import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class H7_111219007 {
    static JFrame frm = new JFrame("憤怒鳥");
    static int ballX = 120, ballY = 550; // 球的初始位置
    static float vx = 0, vy = 0; // 水平與垂直速度
    static Timer timer; // 計時器
    static JPanel pne = new JPanel() {
        Image bgImage = new ImageIcon("src/img/BG.jpg").getImage();
        Image ballImage = new ImageIcon("src/img/RedBird.png").getImage();
        Image shotImage = new ImageIcon("src/img/SlingShot.png").getImage();
        int offsetX, offsetY; // 滑鼠拖曳偏移量
        boolean dragging = false;
        List<Point> trajectory = new ArrayList<>(); //預測路徑

        {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
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
                        repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
            g.drawImage(shotImage, 100, 490, 100, 150, this);
            g.drawImage(ballImage, ballX, ballY, 32, 32, this);

            g.setColor(Color.ORANGE);
            for (int i = 0; i < trajectory.size() - 1; i++) {
                Point p1 = trajectory.get(i);
                Point p2 = trajectory.get(i + 1);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
    };
    static JButton btnReset = new JButton("重置");
    static JButton btnBird = new JButton("切換憤怒鳥");

    public static void main(String[] args) {
        pne.setLayout(null);
        btnReset.setBounds(10, 10, 80, 30);
        btnBird.setBounds(100, 10, 100, 30);
        pne.add(btnReset);
        pne.add(btnBird);

        btnReset.addActionListener(e -> {
            ballX = 120;
            ballY = 550;
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
}
