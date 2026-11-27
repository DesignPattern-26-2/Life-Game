package com.holub.life;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.io.File;
public class IntegratedBoardProbe {
 static void layout(Container c){c.doLayout();for(Component p:c.getComponents())if(p instanceof Container)layout((Container)p);}
 static <T extends Component>T named(Container c,Class<T> kind,String name){for(Component p:c.getComponents()){if(kind.isInstance(p)&&name.equals(p.getName()))return kind.cast(p);if(p instanceof Container){T v=named((Container)p,kind,name);if(v!=null)return v;}}return null;}
 static JToggleButton zoom(Container c){for(Component p:c.getComponents()){if(p instanceof JToggleButton)return (JToggleButton)p;if(p instanceof Container){JToggleButton v=zoom((Container)p);if(v!=null)return v;}}return null;}
 static JTabbedPane tabs(Container c){for(Component p:c.getComponents()){if(p instanceof JTabbedPane)return (JTabbedPane)p;if(p instanceof Container){JTabbedPane v=tabs((Container)p);if(v!=null)return v;}}return null;}
 public static void main(String[] args)throws Exception {
  SwingUtilities.invokeAndWait(()->{
   Cell b=new Neighborhood(8,new Neighborhood(8,new Resident()));
   for(int x=30;x<=32;x++)b.userClicked(new Point(x,31),new Rectangle(64,64));
   LifeHistory h=new LifeHistory(b);long start=System.nanoTime();for(int i=0;i<100100;i++)h.advance();
   System.out.printf("64x64 Blinker, 100k default, fill %.3fs%n",(System.nanoTime()-start)/1e9);
   TimelinePanel p=new TimelinePanel(h,()->{},()->{});StatisticsPanel statistics=new StatisticsPanel(h);
   long[] times=new long[10000];for(int i=0;i<times.length;i++){start=System.nanoTime();h.advance();times[i]=System.nanoTime()-start;}
   MemoryProbe.stats("real Conway+statistics+eviction+both UI updates full 100k",times);
   JPanel root=new JPanel(new BorderLayout());
   JPanel board=new JPanel(){protected void paintComponent(Graphics g){super.paintComponent(g);b.redraw(g,new Rectangle(0,0,512,512),true);}};
   board.setPreferredSize(new Dimension(512,512));root.add(board,BorderLayout.CENTER);root.add(p,BorderLayout.SOUTH);root.add(statistics,BorderLayout.EAST);
   zoom(p).doClick(0);h.seek(55555);
   named(p,JTextField.class,"detail-from").setText("55500");named(p,JTextField.class,"detail-to").setText("55650");named(p,JButton.class,"detail-apply").doClick(0);
   root.setSize(root.getPreferredSize());layout(root);
   BufferedImage image=new BufferedImage(root.getWidth(),root.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
   long[] paints=new long[200];for(int i=0;i<paints.length;i++){start=System.nanoTime();root.printAll(g);paints[i]=System.nanoTime()-start;}
   MemoryProbe.stats("offscreen board+custom timeline+statistics chart",paints);
   try{ImageIO.write(image,"png",new File(args[0]));tabs(statistics).setSelectedIndex(1);layout(root);root.printAll(g);ImageIO.write(image,"png",new File(args[1]));}catch(Exception e){throw new RuntimeException(e);}finally{g.dispose();}
   System.out.println("image size="+image.getWidth()+"x"+image.getHeight());
  });
 }
}
