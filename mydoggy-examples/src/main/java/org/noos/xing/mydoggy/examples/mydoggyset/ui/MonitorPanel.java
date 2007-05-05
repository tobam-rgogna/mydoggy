package org.noos.xing.mydoggy.examples.mydoggyset.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * @author Angelo De Caro
 */
public class MonitorPanel extends JPanel {
    public Surface surf;

    public MonitorPanel(MonitorSource monitorSource) {
        setLayout(new BorderLayout());
        add(surf = createSurface());
        setMonitorSource(monitorSource);
    }

    public void start() {
        surf.start();
    }

    public void stop() {
        surf.stop();
    }

    public MonitorSource getMonitorSource() {
        return surf.getMonitorSource();
    }

    public void setMonitorSource(MonitorSource monitorSource) {
        surf.setMonitorSource(monitorSource);
    }

    protected Surface createSurface() {
        return new Surface(null);
    }

    protected class Surface extends JPanel implements Runnable {
        private MonitorSource monitorSource;
        private Thread thread;

        private long sleepAmount = 100;
        private int w, h;
        private BufferedImage bimg;
        private Graphics2D big;
        private Font font = new Font("Times New Roman", Font.PLAIN, 11);
        private int columnInc;
        private double[] pts;
        private int ptNum;
        private int ascent, descent;
        private Rectangle graphOutlineRect = new Rectangle();
        private Rectangle2D mfRect = new Rectangle2D.Float();
        private Rectangle2D muRect = new Rectangle2D.Float();
        private Line2D graphLine = new Line2D.Float();
        private Color graphColor = new Color(46, 139, 87);
        private Color mfColor = new Color(0, 100, 0);
        private String usedStr;

        public Surface(MonitorSource monitorSource) {
            this.monitorSource = monitorSource;
            setBackground(Color.black);
        }

        public void paint(Graphics g) {
            Dimension d = getSize();
            if (d.width != w || d.height != h) {
                w = d.width;
                h = d.height;
                bimg = (BufferedImage) createImage(w, h);
                big = bimg.createGraphics();
                big.setFont(font);
                FontMetrics fm = big.getFontMetrics(font);
                ascent = fm.getAscent();
                descent = fm.getDescent();
            }

            big.setBackground(getBackground());
            big.clearRect(0, 0, w, h);

            float totalMemory = monitorSource.getTotal();
            float usedMemory = monitorSource.getUsed();
            float freeMemory = totalMemory - usedMemory;

            // .. Draw allocated and used strings ..
            big.setColor(Color.green);
            big.drawString(String.valueOf((int) totalMemory >> 10) + "K allocated", 4.0f, (float) ascent + 0.5f);
            usedStr = String.valueOf(((int) usedMemory) >> 10) + "K used";
            big.drawString(usedStr, 4, h - descent);

            // Calculate remaining size
            float ssH = ascent + descent;
            float remainingHeight = (h - ssH * 2 - 0.5f);
            float blockHeight = remainingHeight / 10;
            float blockWidth = 20.0f;
            float remainingWidth = (w - blockWidth - 10);

            // .. Memory Free ..
            big.setColor(mfColor);
            int MemUsage = (int) (freeMemory / totalMemory * 10);
            int i = 0;
            for (; i < MemUsage; i++) {
                mfRect.setRect(5, ssH + i * blockHeight, blockWidth, blockHeight - 1);
                big.fill(mfRect);
            }

            // .. Memory Used ..
            big.setColor(Color.green);
            for (; i < 10; i++) {
                muRect.setRect(5, ssH + i * blockHeight, blockWidth, blockHeight - 1);
                big.fill(muRect);
            }

            // .. Draw History Graph ..
            big.setColor(graphColor);
            int graphX = 30;
            int graphY = (int) ssH;
            int graphW = w - graphX - 5;
            int graphH = (int) (ssH + (9 * blockHeight) + blockHeight - 1);

            i = 0;
            for (; i < 10; i++) {
                muRect.setRect(graphX, ssH + i * blockHeight - 1, graphW, blockHeight);
                big.draw(muRect);
            }

            // .. Draw animated column movement ..
            int graphColumn = graphW / 15;

            if (columnInc == 0) {
                columnInc = graphColumn;
            }

            for (int j = graphX + columnInc; j < graphW + graphX; j += graphColumn) {
                graphLine.setLine(j, graphY, j, ssH + i * blockHeight - 1);
                big.draw(graphLine);
            }

            --columnInc;

            if (pts == null) {
                pts = new double[graphW];
                ptNum = 0;
            } else if (pts.length != graphW) {
                double[] tmp;
                if (ptNum < graphW) {
                    tmp = new double[ptNum];
                    System.arraycopy(pts, 0, tmp, 0, tmp.length);
                } else {
                    tmp = new double[graphW];
                    System.arraycopy(pts, pts.length - tmp.length, tmp, 0, tmp.length);
                    ptNum = tmp.length - 2;
                }
                pts = new double[graphW];
                System.arraycopy(tmp, 0, pts, 0, tmp.length);
            } else {
                big.setColor(Color.yellow);
                int sum = graphY + graphH;

//                pts[ptNum] = (int) (graphY + graphH * (freeMemory / totalMemory));
                for (int j = sum - ptNum, k = 0; k < ptNum; k++, j++) {
                    if (k != 0) {
                        if (pts[k] != pts[k - 1]) {
                            big.drawLine(j - 1, (int) (sum * pts[k - 1]), j, (int) (sum * pts[k]));
                        } else {
                            big.fillRect(j, (int) (sum * pts[k]), 1, 1);
                        }
                    }
                }
/*
                if (ptNum + 2 == pts.length) {
                    // throw out oldest point
                    System.arraycopy(pts, 1, pts, 0, ptNum);
                    --ptNum;
                } else {
                    ptNum++;
                }
*/
            }

            g.drawImage(bimg, 0, 0, this);
        }

        public void run() {
            // TODO: add closing
            while (true) {
                float totalMemory = monitorSource.getTotal();
                float usedMemory = monitorSource.getUsed();
                float freeMemory = totalMemory - usedMemory;

                if (pts == null) {
                    pts = new double[1];
                    ptNum = 0;
                } else if (pts.length < ptNum + 1) {
                    double[] tmp;
                    int graphW = ptNum+1;
                    if (ptNum < graphW) {
                        tmp = new double[ptNum];
                        System.arraycopy(pts, 0, tmp, 0, tmp.length);
                    } else {
                        tmp = new double[graphW];
                        System.arraycopy(pts, pts.length - tmp.length, tmp, 0, tmp.length);
                        ptNum = tmp.length - 2;
                    }
                    pts = new double[graphW];
                    System.arraycopy(tmp, 0, pts, 0, tmp.length);
                } else {
                    pts[ptNum] = (freeMemory / totalMemory);
                    if (ptNum + 2 == pts.length) {
                        // throw out oldest point
                        System.arraycopy(pts, 1, pts, 0, ptNum);
                        --ptNum;
                    } else {
                        ptNum++;
                    }
                }

//                    if (isShowing())
                    repaint();

                try {
                    Thread.sleep(sleepAmount);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        public void start() {
            if (monitorSource == null)
                throw new IllegalStateException("Monitor Source cannot be null.");

            thread = new Thread(this);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);
            thread.setName("MemoryMonitor");
            thread.start();
        }

        public synchronized void stop() {
            thread = null;
            notify();
        }

        public MonitorSource getMonitorSource() {
            return monitorSource;
        }

        public void setMonitorSource(MonitorSource monitorSource) {
            this.monitorSource = monitorSource;
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getPreferredSize() {
            return new Dimension(135, 80);
        }
    }
}