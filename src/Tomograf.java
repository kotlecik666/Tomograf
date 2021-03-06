import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Tomograf {

    private static Raster raster;

    private static int detectorsNo = 180;
    private static double detectorRange = 270.0;
    private static double emitterStep = 1.0;
    private double startingAngle = Math.toRadians(90.0);

//    private int steps = (int) (180 / emitterStep); // dla 180 stopni
    private int steps;

    private ArrayList<Integer> emitterPositionsX = new ArrayList<>();
    private ArrayList<Integer> emitterPositionsY = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> detectorPositionsX = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> detectorPositionsY = new ArrayList<>();

//    private int[][] sinograph = new int[steps][detectorsNo]; //albo na odwrot
    private int[][] sinograph;

    private int[][] reconstruction;

    private static JFrame frame;

    private void generatePositions(int startingX, int startingY, int radius) {
        double step = Math.toRadians(emitterStep);
        double etdAngle = Math.toRadians(180.0 - (detectorRange / 2.0)); // emitter to 1st detector angle
        double detectorAngle = Math.toRadians(detectorRange / detectorsNo);
        steps = (int) (360 / emitterStep);
        sinograph = new int[steps][detectorsNo];
        for (int i = 0; i < steps; i++) {
            emitterPositionsX.add((int) (startingX + (Math.cos(i * step + startingAngle) * radius))); //i + 1
            emitterPositionsY.add((int) (startingY + (Math.sin(i * step + startingAngle) * radius)));
            ArrayList<Integer> positionsX = new ArrayList<>();
            ArrayList<Integer> positionsY = new ArrayList<>();
            double firstDetectorAngle = ((i * step) + etdAngle);
            for (int j = 0; j < detectorsNo; j++) {
                positionsX.add((int) (startingX + (Math.cos(firstDetectorAngle + startingAngle + (j * detectorAngle)) * radius)));
                positionsY.add((int) (startingY + (Math.sin(firstDetectorAngle + startingAngle + (j * detectorAngle)) * radius)));
            }
            detectorPositionsX.add(positionsX);
            detectorPositionsY.add(positionsY);
        }
    }

    private void bresenham(boolean reconstruct) {
        int d = 0;
        for (int i = 0; i < emitterPositionsX.size(); i++) {
            for (int j = 0; j < detectorPositionsX.get(i).size(); j++) {

                int x1 = emitterPositionsX.get(i);
                int y1 = emitterPositionsY.get(i);
                int x2 = detectorPositionsX.get(i).get(j);
                int y2 = detectorPositionsY.get(i).get(j);

                int dx = Math.abs(x2 - x1);
                int dy = Math.abs(y2 - y1);

                int dx2 = 2 * dx; // slope scaling factors to
                int dy2 = 2 * dy; // avoid floating point

                int ix = x1 < x2 ? 1 : -1; // increment direction
                int iy = y1 < y2 ? 1 : -1;

                int x = x1;
                int y = y1;

                int pixels = 0;

                if (!reconstruct) {
                    if (dx >= dy) {
                        while (true) {
                            sinograph[i][j] += raster.getSample(x, y, 0);
                            pixels++;
                            if (x == x2)
                                break;
                            x += ix;
                            d += dy2;
                            if (d > dx) {
                                y += iy;
                                d -= dx2;
                            }
                        }
                    } else {
                        while (true) {
                            sinograph[i][j] += raster.getSample(x, y, 0);
                            pixels++;
                            if (y == y2)
                                break;
                            y += iy;
                            d += dx2;
                            if (d > dy) {
                                x += ix;
                                d -= dy2;
                            }
                        }
                    }
                    sinograph[i][j] /= pixels;
                } else {
                    if (dx >= dy) {
                        while (true) {
//                            sinograph[i][j] += raster.getSample(x, y, 0);
                            reconstruction[x][y] += sinograph[i][j];
                            pixels++;
                            if (x == x2)
                                break;
                            x += ix;
                            d += dy2;
                            if (d > dx) {
                                y += iy;
                                d -= dx2;
                            }
                        }
                    } else {
                        while (true) {
//                            sinograph[i][j] += raster.getSample(x, y, 0);
                            reconstruction[x][y] += sinograph[i][j];
                            pixels++;
                            if (y == y2)
                                break;
                            y += iy;
                            d += dx2;
                            if (d > dy) {
                                x += ix;
                                d -= dy2;
                            }
                        }
                    }
                }
            }
        }
        double max = 0;
        if (!reconstruct) {
            for (int i = 0; i < sinograph.length; i++) {
                for (int j = 0; j < sinograph[0].length; j++) {
                    if (sinograph[i][j] > max) max = sinograph[i][j];
                }
            }
            for (int i = 0; i < sinograph.length; i++) {
                for (int j = 0; j < sinograph[0].length; j++) {
                    sinograph[i][j] = (int)((sinograph[i][j] / max) * 255);
                }
            }
        }
        if (reconstruct) {
            for (int i = 0; i < reconstruction.length; i++) {
                for (int j = 0; j < reconstruction.length; j++) {
                    reconstruction[i][j] /= steps;
                }
            }
        }
    }

    private BufferedImage writeImage() {
        String path = "res/output.png";
//        BufferedImage image = new BufferedImage(sinograph.length, sinograph[0].length, BufferedImage.TYPE_INT_RGB);
        BufferedImage image = new BufferedImage(sinograph.length, sinograph[0].length, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < steps; x++) {
            for (int y = 0; y < detectorsNo; y++) {
                Color c = new Color(sinograph[x][y], sinograph[x][y], sinograph[x][y]);
//                image.setRGB(x, y, sinograph[x][y]);
                image.setRGB(x, y, c.getRGB());
            }
        }

        File ImageFile = new File(path);
        try {
            ImageIO.write(image, "png", ImageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Zapisano obraz.");

        return image;
    }

    private BufferedImage writeReconstruction(int size) {
        String path = "res/reconstruction.png";
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Color c = new Color(reconstruction[x][y], reconstruction[x][y], reconstruction[x][y]);
                image.setRGB(x, y, c.getRGB());
            }
        }

        File ImageFile = new File(path);
        try {
            ImageIO.write(image, "png", ImageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Zapisano rekonstrukcje.");

        return image;
    }

    public void caclutalte(BufferedImage image, ArrayList <Integer> list){
        detectorsNo = list.get(0);
        detectorRange = (double) list.get(1).intValue();
        emitterStep = ((double) list.get(2).intValue()) / 100.0;
        frame.setVisible(false);
        frame.dispose();
        detectorPositionsX.clear();
        detectorPositionsY.clear();
        emitterPositionsX.clear();
        emitterPositionsY.clear();
        Tomograf tomo = this;
        int size = image.getHeight();
        raster = image.getData();
        tomo.generatePositions(size/2, size/2, size/2-1);
        sinograph = new int[steps][detectorsNo];
        tomo.reconstruction = new int[size][size];
        tomo.bresenham(false);
        tomo.bresenham(true);

        BufferedImage sin_img = tomo.writeImage();
        BufferedImage wyj_img = tomo.writeReconstruction(size);


        frame = new JFrame("Tomografia");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new MyPanel(tomo, tomo.emitterPositionsX.get(0), tomo.emitterPositionsY.get(0),
                tomo.detectorPositionsX.get(0), tomo.detectorPositionsY.get(0), tomo.detectorsNo, size, image, sin_img, wyj_img, detectorsNo, detectorRange, emitterStep, estimate_error(image, wyj_img)));
        //frame.setSize(size+200, size+200);
        frame.pack();
        frame.setVisible(true);
    }

    public static double estimate_error(BufferedImage a, BufferedImage b){
        int size = a.getHeight();
        double sum = 0.0;
        for(int i = 0; i < size; ++i){
            for(int j = 0; j < size; ++j){
                sum += Math.pow((a.getRGB(i, j) & 0x000000ff) - (b.getRGB(i, j) & 0x000000ff), 2.0);
            }
        }
        sum /= Math.pow(size, 2.0);
        return Math.sqrt(sum);
    }

    public static void main(String[] args) throws IOException {
        File file = new File("./shepp256.png");
        BufferedImage image = ImageIO.read(file);
        int size = image.getHeight();

        raster = image.getData();

        Tomograf tomo = new Tomograf();
        tomo.generatePositions(size/2, size/2, size/2-1);

        tomo.reconstruction = new int[size][size];
        tomo.bresenham(false);
        tomo.bresenham(true);

        BufferedImage sin_img = tomo.writeImage();
        BufferedImage wyj_img = tomo.writeReconstruction(size);

        frame = new JFrame("Tomografia");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new MyPanel(tomo, tomo.emitterPositionsX.get(0), tomo.emitterPositionsY.get(0),
                tomo.detectorPositionsX.get(0), tomo.detectorPositionsY.get(0), tomo.detectorsNo, size, image, sin_img, wyj_img, detectorsNo, detectorRange, emitterStep, estimate_error(image, wyj_img)));
        //frame.setSize(size+200, size+200);
        frame.pack();
        frame.setVisible(true);
    }

}
