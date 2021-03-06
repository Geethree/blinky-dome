package com.github.starcats.blinkydome.util;

import com.github.dlopuch.apa102_java_rpi.Apa102Output;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.output.LXOutput;

import java.io.IOException;

/**
 * LXOutput directly driving APA102's on a Raspi.
 *
 * See https://github.com/dlopuch/apa102-java-rpi
 */
public class Apa102RpiOutput extends LXOutput {
  private final Apa102Output output;
  private final byte[] outputRgbBuffer;

  /**
   * Initialize a new output against a fully-initialized model
   * @param lx lx.model.points must be fully initialized
   */
  public Apa102RpiOutput(LX lx) {
    super(lx);

    try {
      Apa102Output.initSpi(SpiChannel.CS0, 7800000, SpiDevice.DEFAULT_SPI_MODE);
    } catch (IOException e) {
      System.out.println("ERROR: Error initializing APA output (probably no sudo or not a raspi).  Ignoring APA output and moving on:");
      System.out.println(e);

      this.output = null;
      this.outputRgbBuffer = null;

      return;
    }
    output = new Apa102Output(lx.model.points.length);
    outputRgbBuffer = new byte[ lx.model.points.length * 3 ];
    System.out.println("APA102 SPI output initialized successfully.");
  }

  @Override
  protected void onSend(int[] colors) {
    if (this.output == null) {
      return;
    }
    // we assume colors is same length as model.points array that we initialized buffer against

    if (colors.length * 3 != outputRgbBuffer.length) {
      throw new RuntimeException("Bad assumption: expects colors length (" + colors.length + ") to equal 1/3 of " +
          "outputRgbBuffer length (from model, " + (outputRgbBuffer.length / 3) + ")");
    }

    int colorsI = 0;
    for (int i=0; i<outputRgbBuffer.length; ) {
      outputRgbBuffer[ i++ ] = LXColor.red(   colors[ colorsI ]);
      outputRgbBuffer[ i++ ] = LXColor.green( colors[ colorsI ]);
      outputRgbBuffer[ i++ ] = LXColor.blue(  colors[ colorsI ]);
      colorsI++;
    }

    try {
      output.writeStrip(outputRgbBuffer);
    } catch (IOException e) {
      throw new RuntimeException("Error writing output strip", e);
    }
  }
}
