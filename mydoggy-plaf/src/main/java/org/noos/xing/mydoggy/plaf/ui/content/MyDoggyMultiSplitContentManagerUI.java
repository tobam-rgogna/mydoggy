package org.noos.xing.mydoggy.plaf.ui.content;

import org.noos.xing.mydoggy.*;
import org.noos.xing.mydoggy.plaf.MyDoggyContentManager;
import org.noos.xing.mydoggy.plaf.MyDoggyToolWindowManager;
import org.noos.xing.mydoggy.plaf.ui.ResourceManager;
import org.noos.xing.mydoggy.plaf.ui.cmp.MultiSplitTabDockableContainer;
import org.noos.xing.mydoggy.plaf.ui.cmp.MultiSplitDockableContainer;
import org.noos.xing.mydoggy.plaf.ui.content.action.NextContentAction;
import org.noos.xing.mydoggy.plaf.ui.content.action.PreviousContentAction;
import org.noos.xing.mydoggy.plaf.ui.util.SwingUtil;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Hashtable;
import java.util.Map;

/**
 * @author Angelo De Caro (angelo.decaro@gmail.com)
 */
public class MyDoggyMultiSplitContentManagerUI implements MultiSplitContentManagerUI, PlafContentManagerUI, PropertyChangeListener {
    protected MyDoggyToolWindowManager toolWindowManager;
    protected MyDoggyContentManager contentManager;
    protected ResourceManager resourceManager;

    protected MultiSplitDockableContainer dockableContainer;
    protected boolean closeable, detachable;
    protected boolean installed;

    protected PropertyChangeSupport propertyChangeSupport;
    protected EventListenerList contentManagerUIListeners;

    protected PlafContentUI lastSelected;

    protected boolean valueAdjusting;
    protected boolean contentValueAdjusting;

    protected Map<Content, MultiSplitContentUI> detachedContentUIMap;

    protected int contentIndex = 0;

    protected JPopupMenu popupMenu;


    public MyDoggyMultiSplitContentManagerUI() {
        this.closeable = this.detachable = true;
    }


    public void setCloseable(boolean closeable) {
        this.closeable = closeable;
//        if (multiSplitPane != null)
//            for (JInternalFrame frame : multiSplitPane.getAllFrames()) {
//                frame.setClosable(closeable);
//            }
    }

    public void setDetachable(boolean detachable) {
        this.detachable = detachable;
//        if (multiSplitPane != null)
//            for (JInternalFrame internalFrame : multiSplitPane.getAllFrames()) {
//                DesktopContentFrame frame = (DesktopContentFrame) internalFrame;
//                frame.setDetachable(detachable);
//            }
    }

    public MultiSplitContentUI getContentUI(Content content) {
        if (content.isDetached()) {
            return detachedContentUIMap.get(content);
        } /*else
            return (DesktopContentUI) getFrameByComponent(content.getComponent());*/
        return null;
    }


    public Container getContainer() {
        return dockableContainer;
    }

    public PlafContentManagerUI install(ContentManagerUI oldContentManagerUI, ToolWindowManager manager) {
        this.toolWindowManager = (MyDoggyToolWindowManager) manager;
        this.contentManager = (MyDoggyContentManager) manager.getContentManager();
        this.resourceManager = toolWindowManager.getResourceManager();
        this.contentIndex = 0;

        initComponents();
        initListeners();
        setupActions();

        toolWindowManager.setMainContent(dockableContainer);

        setPopupMenu(contentManager.getPopupMenu());

        contentValueAdjusting = true;
        Content selectedContent = null;
        for (Content content : contentManager.getContents()) {
            if (content.isSelected())
                selectedContent = content;
            addContent((PlafContentUI) content);
        }
        contentValueAdjusting = false;

        if (oldContentManagerUI != null) {
            for (ContentManagerUIListener listener : oldContentManagerUI.getContentManagerUiListener()) {
                addContentManagerUIListener(listener);
            }
        }

        this.installed = true;

        final Content selectedContent1 = selectedContent;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (selectedContent1 != null)
                    selectedContent1.setSelected(true);
                else if (contentManager.getContentCount() > 0) {
                    contentManager.getContent(0).setSelected(true);
                }
            }
        });

        return this;
    }

    public void unistall() {
        for (Content content : contentManager.getContents()) {
            removeContent((PlafContentUI) content);
        }
        this.installed = false;
    }

    public boolean isInstalled() {
        return installed;
    }

    public void addContent(PlafContentUI content, Object... constraints) {
        addUIForContent(content, constraints);
        content.addUIPropertyChangeListener(this);
    }

    public void removeContent(PlafContentUI content) {
        if (content.isDetached())
            content.setDetached(false);

//        for (JInternalFrame internalFrame : multiSplitPane.getAllFrames()) {
//            if (internalFrame.getContentPane().getComponent(0) == content.getComponent()) {
//                multiSplitPane.remove(internalFrame);
//                break;
//            }
//        }

        content.removeUIPropertyChangeListener(this);
    }

    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }

    public void setPopupMenu(JPopupMenu popupMenu) {
        this.popupMenu = popupMenu;
    }

    public boolean isSelected(Content content) {
        return content == lastSelected;
    }

    public void setSelected(Content content, boolean selected) {
        if (content.isDetached()) {
            SwingUtil.requestFocus(
                    SwingUtilities.windowForComponent(content.getComponent())
            );
        } else {
//            JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
//            if (internalFrame != null)
//                try {
//                    valueAdjusting = true;
//                    internalFrame.setSelected(selected);
//                    lastSelected = (PlafContentUI) content;
//                    valueAdjusting = false;
//                } catch (PropertyVetoException e) {
//                    e.printStackTrace();
//                }
//            else
//                throw new IllegalStateException("Invalid content ui state.");
        }
    }

    public void updateUI() {
        dockableContainer.updateUI();
    }

    public void addContentManagerUIListener(ContentManagerUIListener listener) {
        contentManagerUIListeners.add(ContentManagerUIListener.class, listener);
    }

    public void removeContentManagerUIListener(ContentManagerUIListener listener) {
        contentManagerUIListeners.remove(ContentManagerUIListener.class, listener);
    }

    public ContentManagerUIListener[] getContentManagerUiListener() {
        return contentManagerUIListeners.getListeners(ContentManagerUIListener.class);
    }


    public void propertyChange(PropertyChangeEvent evt) {
        propertyChangeSupport.firePropertyChange(evt);
    }


    protected void initComponents() {
        if (dockableContainer == null) {
            detachedContentUIMap = new Hashtable<Content, MultiSplitContentUI>();
            dockableContainer = new MultiSplitTabDockableContainer(toolWindowManager);
        }
    }

    protected void initListeners() {
        if (propertyChangeSupport == null) {
            propertyChangeSupport = new PropertyChangeSupport(this);
            propertyChangeSupport.addPropertyChangeListener("component", new ComponentListener());
            propertyChangeSupport.addPropertyChangeListener("disabledIcon", new DisabledIconListener());
            propertyChangeSupport.addPropertyChangeListener("icon", new IconListener());
            propertyChangeSupport.addPropertyChangeListener("enabled", new EnabledListener());
            propertyChangeSupport.addPropertyChangeListener("foreground", new ForegroundListener());
            propertyChangeSupport.addPropertyChangeListener("popupMenu", new PopupMenuListener());
            propertyChangeSupport.addPropertyChangeListener("title", new TitleListener());
            propertyChangeSupport.addPropertyChangeListener("toolTipText", new ToolTipTextListener());
            propertyChangeSupport.addPropertyChangeListener("detached", new DetachedListener());
            propertyChangeSupport.addPropertyChangeListener("selected", new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    //                System.out.println("SELECTED " + evt.getNewValue());
                }
            });
        }
        this.contentManagerUIListeners = new EventListenerList();
    }

    protected void setupActions() {
        // Setup actions
        SwingUtil.addKeyActionMapping(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, dockableContainer,
                                      KeyStroke.getKeyStroke(39, InputEvent.ALT_MASK),
                                      "nextContent", new NextContentAction(toolWindowManager));
        SwingUtil.addKeyActionMapping(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, dockableContainer,
                                      KeyStroke.getKeyStroke(37, InputEvent.ALT_MASK),
                                      "previousContent", new PreviousContentAction(toolWindowManager));
    }

    protected void addUIForContent(Content content, Object... constraints) {
        if (constraints == null || constraints.length == 0) {
            dockableContainer.addContent(content,
                                         content.getComponent(),
                                         null,
                                         AggregationPosition.DEFAULT);
        } else {
        }
        SwingUtil.repaint(dockableContainer);
    }

    protected class ComponentListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();

            if (content.isDetached()) {
                RootPaneContainer rootPaneContainer = (RootPaneContainer) SwingUtilities.windowForComponent(content.getComponent());
                Container container = rootPaneContainer.getContentPane();
                container.removeAll();
                container.add((Component) evt.getNewValue());
            } else {
//                JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
//                if (internalFrame != null) {
//                    Container container = internalFrame.getContentPane();
//                    container.removeAll();
//                    container.add((Component) evt.getNewValue());
//                } else
//                    throw new IllegalStateException("Invalid content ui state.");
            }
        }
    }

    protected class DisabledIconListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
        }
    }

    protected class IconListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();

            if (!content.isDetached()) {
//                JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
//                if (internalFrame != null)
//                    internalFrame.setFrameIcon((Icon) evt.getNewValue());
//                else
//                    throw new IllegalStateException("Invalid content ui state.");
            }
        }
    }

    protected class EnabledListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();

            if (!content.isDetached()) {
//                JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
//                if (internalFrame != null) {
//                    internalFrame.setEnabled((Boolean) evt.getNewValue());
//                } else
//                    throw new IllegalStateException("Invalid content ui state.");
            }
        }
    }

    protected class ForegroundListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();

            if (!content.isDetached()) {
//                JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
//                if (internalFrame != null)
//                    internalFrame.setForeground((Color) evt.getNewValue());
//                else
//                    throw new IllegalStateException("Invalid content ui state.");
            }
        }
    }

    protected class PopupMenuListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();

            if (!content.isDetached()) {
//                JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
//                if (internalFrame != null)
//                    internalFrame.setComponentPopupMenu((JPopupMenu) evt.getNewValue());
//                else
//                    throw new IllegalStateException("Invalid content ui state.");
            }
        }
    }

    protected class TitleListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();

            if (content.isDetached()) {
                JDialog dialog = (JDialog) SwingUtilities.windowForComponent(content.getComponent());
                dialog.setTitle((String) evt.getNewValue());
            } else {
//                JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
//                if (internalFrame != null)
//                    internalFrame.setTitle((String) evt.getNewValue());
//                else
//                    throw new IllegalStateException("Invalid content ui state.");
            }
        }
    }

    protected class ToolTipTextListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            Content content = (Content) evt.getSource();

            if (!content.isDetached()) {
//                JInternalFrame internalFrame = getFrameByComponent(content.getComponent());
//                if (internalFrame != null) {
//                    String newToolTip = (String) evt.getNewValue();
//                    if (newToolTip == null)
//                        newToolTip = "";
//
//                    internalFrame.setToolTipText(newToolTip);
//                } else
//                    throw new IllegalStateException("Invalid content ui state.");
            }
        }
    }

    protected class DetachedListener implements PropertyChangeListener {
        protected Frame parentFrame;

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

                Window parentWindow = SwingUtilities.windowForComponent(dockableContainer);
                Component component = content.getComponent();

//                JInternalFrame internalFrame = getFrameByComponent(component);
//                if (internalFrame != null) {
//                    multiSplitPane.remove(internalFrame);
//                    detachedContentUIMap.put(content, (DesktopContentUI) internalFrame);
//                } else
//                    throw new IllegalStateException("Invalid Content : " + content);
//
//                component.setPreferredSize(component.getSize());
//
//                dialog.setTitle(content.getTitle());
//                dialog.getContentPane().add(component);
//
//                Point location = parentWindow.getLocation();
//                location.x += 5;
//                location.y += 5;
//                dialog.setLocation(location);
//
//                dialog.pack();
//
//                if (resourceManager.getTransparencyManager().isServiceAvailable()) {
//                    WindowTransparencyListener windowTransparencyListener = new WindowTransparencyListener(
//                            resourceManager.getTransparencyManager(),
//                            getContentUI(content),
//                            dialog
//                    );
//                    dialog.addWindowListener(windowTransparencyListener);
//                    dialog.addWindowFocusListener(windowTransparencyListener);
//                }
//
//                dialog.addWindowListener(new WindowAdapter() {
//                    public void windowClosing(WindowEvent event) {
//                        Component component = dialog.getContentPane().getComponent(0);
//                        PlafContentUI content = (PlafContentUI) contentManager.getContentByComponent(component);
//                        content.fireSelected(false);
//                        content.setDetached(false);
//                    }
//                });
//
//                dialog.addWindowFocusListener(new WindowFocusListener() {
//                    public void windowGainedFocus(WindowEvent e) {
//                        if (!valueAdjusting && !contentValueAdjusting) {
//                            PlafContentUI newSelected = (PlafContentUI) contentManager.getContentByComponent(
//                                    dialog.getContentPane().getComponent(0));
//
//                            if (newSelected == lastSelected)
//                                return;
//
//                            if (lastSelected != null) {
//                                try {
//                                    getFrameByComponent(lastSelected.getComponent()).setSelected(false);
////                                    lastSelected.fireSelected(false);
//                                } catch (Exception ignoreIt) {
//                                }
//                            }
//
//                            lastSelected = newSelected;
//                            newSelected.fireSelected(true);
//                        }
//                    }
//
//                    public void windowLostFocus(WindowEvent e) {
//                    }
//                });
//
//                if (parentFrame == null)
//                    dialog.addWindowFocusListener(new ToFrontWindowFocusListener(dialog));
//
//                dialog.toFront();
//                dialog.setVisible(true);
//                SwingUtil.repaint(multiSplitPane);
//                SwingUtil.requestFocus(dialog);
            } else if (oldValue && !newValue) {
                Window window = SwingUtilities.windowForComponent(content.getComponent());
                window.setVisible(false);
                window.dispose();

                addUIForContent(content);
                content.setSelected(true);
            }
        }

    }

}