package org.noos.xing.mydoggy.plaf.ui.content.desktop;

import org.noos.xing.mydoggy.Content;
import org.noos.xing.mydoggy.DesktopContentUI;
import org.noos.xing.mydoggy.DesktopContentManagerUI;
import org.noos.xing.mydoggy.ToolWindowManager;
import org.noos.xing.mydoggy.plaf.MyDoggyContentManager;
import org.noos.xing.mydoggy.plaf.MyDoggyToolWindowManager;
import org.noos.xing.mydoggy.plaf.support.PropertyChangeSupport;
import org.noos.xing.mydoggy.plaf.ui.TransparencyAnimation;
import org.noos.xing.mydoggy.plaf.ui.content.ContentManagerUI;
import org.noos.xing.mydoggy.plaf.ui.content.ContentUI;
import org.noos.xing.mydoggy.plaf.ui.transparency.TransparencyManager;
import org.noos.xing.mydoggy.plaf.ui.util.SwingUtil;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

/**
 * @author Angelo De Caro (angelo.decaro@gmail.com)
 */
public class MyDoggyDesktopContentManagerUI implements DesktopContentManagerUI, ContentManagerUI, PropertyChangeListener {
    private MyDoggyToolWindowManager toolWindowManager;
    private MyDoggyContentManager contentManager;

    private JDesktopPane desktopPane;

    private PropertyChangeSupport propertyChangeSupport;

    boolean valueAdjusting;
    boolean contentValueAdjusting;

    public MyDoggyDesktopContentManagerUI() {
        initComponents();
    }


    public DesktopContentUI getDesktopContentUI(Content content) {
        return (DesktopContentUI) getFrameByComponent(content.getComponent());
    }


    public void install(ToolWindowManager manager) {
        this.toolWindowManager = (MyDoggyToolWindowManager) manager;
        this.contentManager = (MyDoggyContentManager) manager.getContentManager();

        initListeners();

        contentValueAdjusting = true;
        for (Content content : contentManager.getContents()) {
            addContent((ContentUI) content);
        }
        contentValueAdjusting = false;

        if (contentManager.getContentCount() > 0) {
            contentManager.getContent(0).setSelected(true);
        }
    }

    public void unistall() {
        for (Content content : contentManager.getContents()) {
            removeContent((ContentUI) content);
        }
    }

    public void addContent(ContentUI content) {
        addUIForContent(content);
        content.addUIPropertyChangeListener(this);
    }

    public void removeContent(ContentUI content) {
        if (content.isDetached())
            content.setDetached(false);

        for (JInternalFrame internalFrame : desktopPane.getAllFrames()) {
            if (internalFrame.getContentPane().getComponent(0) == content.getComponent()) {
                desktopPane.remove(internalFrame);
                break;
            }
        }

        content.removeUIPropertyChangeListener(this);
    }

    public JPopupMenu getPopupMenu() {
//        return desktopPane.getPopupMenu();    // TODO: che fare del PopupMenu
        return null;
    }

    public void setPopupMenu(JPopupMenu popupMenu) {
//        desktopPane.getUI().setPopupMenu(popupMenu);
    }

    public boolean isSelected(Content content) {
        if (content.isDetached()) {
            return SwingUtilities.windowForComponent(content.getComponent()).isFocused();
        } else {
            JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
            if (internalFrame != null)
                return internalFrame.isSelected();
            else {
                if (contentValueAdjusting)
                    return false;
                else {
                    if (contentManager.getContent(content.getKey()) != null)
                        return false;
                    else
                        throw new IllegalStateException("Invalid content ui state.");
                }
            }
        }
    }

    public void setSelected(Content content, boolean selected) {
        if (content.isDetached()) {
            SwingUtil.requestFocus(
                    SwingUtilities.windowForComponent(content.getComponent())
            );
        } else {
            JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
            if (internalFrame != null)
                try {
                    valueAdjusting = true;
                    internalFrame.setSelected(selected);
                    valueAdjusting = false;
                } catch (PropertyVetoException e) {
                    e.printStackTrace();
                }
            else
                throw new IllegalStateException("Invalid content ui state.");
        }
    }

    public void updateUI() {
        desktopPane.updateUI();
    }


    public void propertyChange(PropertyChangeEvent evt) {
        propertyChangeSupport.firePropertyChangeEvent(evt);
    }


    protected void initComponents() {
        this.desktopPane = new JDesktopPane();
        this.desktopPane.setDesktopManager(new ContentDesktopManager());
    }

    protected void initListeners() {
        if (propertyChangeSupport == null) {
            propertyChangeSupport = new PropertyChangeSupport();
            propertyChangeSupport.addPropertyChangeListener("component", new MyDoggyDesktopContentManagerUI.ComponentListener());
            propertyChangeSupport.addPropertyChangeListener("disabledIcon", new MyDoggyDesktopContentManagerUI.DisabledIconListener());
            propertyChangeSupport.addPropertyChangeListener("icon", new MyDoggyDesktopContentManagerUI.IconListener());
            propertyChangeSupport.addPropertyChangeListener("enabled", new MyDoggyDesktopContentManagerUI.EnabledListener());
            propertyChangeSupport.addPropertyChangeListener("foreground", new MyDoggyDesktopContentManagerUI.ForegroundListener());
            propertyChangeSupport.addPropertyChangeListener("popupMenu", new MyDoggyDesktopContentManagerUI.PopupMenuListener());
            propertyChangeSupport.addPropertyChangeListener("title", new MyDoggyDesktopContentManagerUI.TitleListener());
            propertyChangeSupport.addPropertyChangeListener("toolTipText", new MyDoggyDesktopContentManagerUI.ToolTipTextListener());
            propertyChangeSupport.addPropertyChangeListener("detached", new MyDoggyDesktopContentManagerUI.DetachedListener());
            propertyChangeSupport.addPropertyChangeListener("selected", new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
    //                System.out.println("SELECTED " + evt.getNewValue());
                }
            });
        }
    }

    protected void addUIForContent(Content content) {
        JInternalFrame internalFrame = new DesktopContentFrame(content.getTitle(), true, true, true, true);
        internalFrame.setFrameIcon(content.getIcon());
        internalFrame.getContentPane().add(content.getComponent());
        internalFrame.setBounds(10, 10, 320, 200);   // TODO: lasciare al desktop manager questa scelta??
        internalFrame.addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosed(InternalFrameEvent e) {
                contentManager.removeContent(
                        contentManager.getContent(e.getInternalFrame().getContentPane().getComponent(0))
                );
            }
        });
        internalFrame.addPropertyChangeListener(JInternalFrame.IS_SELECTED_PROPERTY, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (!valueAdjusting && !contentValueAdjusting) {
                    Container container = ((JInternalFrame) evt.getSource()).getContentPane();
                    if (container.getComponentCount() > 0) {
                        Component cmp = container.getComponent(0);
                        for (Content content : contentManager.getContents()) {
                            if (content.getComponent() == cmp) {
                                ((ContentUI) content).fireSelected((Boolean) evt.getNewValue());
                                break;
                            }
                        }
                    }
                }
            }
        });

        desktopPane.add(internalFrame);

        internalFrame.show();

        toolWindowManager.setMainContent(desktopPane);
    }

    protected JInternalFrame getFrameByComponent(Component component) {
        for (JInternalFrame internalFrame : desktopPane.getAllFrames()) {
            if (internalFrame.getContentPane().getComponent(0) == component)
                return internalFrame;
        }
        return null;
    }


    class ComponentListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();

            if (content.isDetached()) {
                RootPaneContainer rootPaneContainer = (RootPaneContainer) SwingUtilities.windowForComponent(content.getComponent());
                Container container = rootPaneContainer.getContentPane();
                container.removeAll();
                container.add((Component) evt.getNewValue());
            } else {
                JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
                if (internalFrame != null) {
                    Container container = internalFrame.getContentPane();
                    container.removeAll();
                    container.add((Component) evt.getNewValue());
                } else
                    throw new IllegalStateException("Invalid content ui state.");
            }
        }
    }

    class DisabledIconListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            // TODO: in which way can i support this???
        }
    }

    class IconListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();

            if (!content.isDetached()) {
                JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
                if (internalFrame != null)
                    internalFrame.setFrameIcon((Icon) evt.getNewValue());
                else 
                    throw new IllegalStateException("Invalid content ui state.");
            }
        }
    }

    class EnabledListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();

            if (!content.isDetached()) {
                JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
                if (internalFrame != null) {
                    internalFrame.setEnabled((Boolean) evt.getNewValue());
                } else
                    throw new IllegalStateException("Invalid content ui state.");
            }
        }
    }

    class ForegroundListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();

            if (!content.isDetached()) {
                JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
                if (internalFrame != null)
                    internalFrame.setForeground((Color) evt.getNewValue());
                else
                    throw new IllegalStateException("Invalid content ui state.");
            }
        }
    }

    class PopupMenuListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();

            if (!content.isDetached()) {
                JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
                if (internalFrame != null)
                    internalFrame.setComponentPopupMenu((JPopupMenu) evt.getNewValue());
                else
                    throw new IllegalStateException("Invalid content ui state.");
            }
        }
    }

    class TitleListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();

            if (content.isDetached()) {
                JDialog dialog = (JDialog) SwingUtilities.windowForComponent(content.getComponent());
                dialog.setTitle((String) evt.getNewValue());
            } else {
                JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
                if (internalFrame != null)
                    internalFrame.setTitle((String) evt.getNewValue());
                else
                    throw new IllegalStateException("Invalid content ui state.");
            }
        }
    }

    class ToolTipTextListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();

            if (!content.isDetached()) {
                JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
                if (internalFrame != null) {
                    String newToolTip = (String) evt.getNewValue();
                    if (newToolTip == null)
                        newToolTip = "";

                    internalFrame.setToolTipText(newToolTip);
                } else
                    throw new IllegalStateException("Invalid content ui state.");
            }
        }
    }

    class DetachedListener implements PropertyChangeListener {
        private Frame parentFrame;

        public DetachedListener() {
            parentFrame = (toolWindowManager.getAnchestor() instanceof Frame) ? (Frame) toolWindowManager.getAnchestor() : null;
        }

        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();
            boolean oldValue = (Boolean) evt.getOldValue();
            boolean newValue = (Boolean) evt.getNewValue();

            if (!oldValue && newValue) {
                final JDialog dialog = new JDialog(parentFrame, false);
                dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

                Window parentWindow = SwingUtilities.windowForComponent(desktopPane);
                Component component = content.getComponent();

                JInternalFrame internalFrame = getFrameByComponent(component);
                if (internalFrame != null) {
                    desktopPane.remove(internalFrame);
                } else
                    throw new IllegalStateException("Invalid Content : " + content);

                component.setPreferredSize(component.getSize());

                dialog.setTitle(content.getTitle());
                dialog.getContentPane().add(component);

                Point location = parentWindow.getLocation();
                location.x += 5;
                location.y += 5;
                dialog.setLocation(location);

                dialog.pack();

                if (TransparencyManager.getInstance().isServiceAvailable()) {
                    MyDoggyDesktopContentManagerUI.DetachedListener.TransparencyListener transparencyListener = new MyDoggyDesktopContentManagerUI.DetachedListener.TransparencyListener(dialog);
                    dialog.addWindowListener(transparencyListener);
                    dialog.addWindowFocusListener(transparencyListener);
                }

                dialog.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent event) {
                        Component component = dialog.getContentPane().getComponent(0);
                        Content content = contentManager.getContent(component);
                        content.setDetached(false);
                    }
                });

                if (parentFrame == null) {
                    WindowFocusListener windowFocusListener = new WindowFocusListener() {
                        long start;
                        long end;

                        public void windowGainedFocus(WindowEvent e) {
                            start = System.currentTimeMillis();
                        }

                        public void windowLostFocus(WindowEvent e) {
                            end = System.currentTimeMillis();
                            long elapsed = end - start;
                            //System.out.println(elapsed);
                            if (elapsed < 100)
                                dialog.toFront();

                            dialog.removeWindowFocusListener(this);
                        }
                    };
                    dialog.addWindowFocusListener(windowFocusListener);
                }

                dialog.toFront();
                dialog.setVisible(true);
                SwingUtil.repaint(desktopPane);
                SwingUtil.requestFocus(dialog);
            } else if (oldValue && !newValue) {
                Window window = SwingUtilities.windowForComponent(content.getComponent());
                window.setVisible(false);
                window.dispose();

                addUIForContent(content);
                content.setSelected(true);
            }
        }

        class TransparencyListener extends WindowAdapter implements WindowFocusListener, ActionListener {
            private final TransparencyManager transparencyManager = TransparencyManager.getInstance();

            private TransparencyAnimation animation;

            private Timer timer;
            private Window window;

            public TransparencyListener(Window window) {
                this.window = window;
                this.animation = new TransparencyAnimation(window, 0.8f);
            }

            public void windowGainedFocus(WindowEvent e) {
                if (transparencyManager.isAlphaModeEnabled(e.getWindow())) {
                    timer.stop();
                    animation.hide();
                    transparencyManager.setAlphaModeRatio(e.getWindow(), 0.0f);
                }
            }

            public void windowLostFocus(WindowEvent e) {
                if (!transparencyManager.isAlphaModeEnabled(e.getWindow())) {
                    timer = new Timer(2000, this);
                    timer.start();
                }
            }

            public void actionPerformed(ActionEvent e) {
                if (timer.isRunning()) {
                    timer.stop();
                    synchronized (transparencyManager) {
                        animation.show();
                    }
                }
            }

            public void windowClosing(WindowEvent event) {
                if (transparencyManager.isAlphaModeEnabled(event.getWindow())) {
                    animation.hide();
                    transparencyManager.setAlphaModeRatio(window, 0.0f);
                }
            }

        }
    }


    class DesktopContentFrame extends JInternalFrame implements DesktopContentUI {

        public DesktopContentFrame(String title, boolean resizable, boolean closable, boolean maximizable, boolean iconifiable) {
            super(title, resizable, closable, maximizable, iconifiable);
        }

        public boolean isIconified() {
            return super.isIcon();
        }

        public void setIconified(boolean iconified) {
            try {
                setIcon(iconified);
            } catch (PropertyVetoException ignore) {
                ignore.printStackTrace();
            }
        }
    }
}