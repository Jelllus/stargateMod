import javax.imageio.ImageIO; import java.awt.*; import java.awt.image.BufferedImage; import java.io.File;
/** ToPng in out size  |  ToPng --white out size */
public class ToPng { public static void main(String[] a) throws Exception {
  int size = Integer.parseInt(a[2]); BufferedImage dst = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
  Graphics2D g = dst.createGraphics();
  if (a[0].equals("--white")) { g.setColor(Color.WHITE); g.fillRect(0, 0, size, size); }
  else { g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
         g.drawImage(ImageIO.read(new File(a[0])), 0, 0, size, size, null); }
  g.dispose(); ImageIO.write(dst, "png", new File(a[1])); System.out.println(a[1] + " " + size); } }
