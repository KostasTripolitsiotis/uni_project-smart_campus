package uni.smartcampus.util;

import java.awt.Color;
import java.time.format.DateTimeFormatter;

public final class UIConstants {
  private UIConstants(){}

  public static final String FONT = "SansSerif";

  public static final Color BG_APP      = new Color(240, 242, 245);
  public static final Color BG_HEADER   = new Color(30, 40, 60);
  public static final Color BG_PANEL    = new Color(248, 249, 252);
  public static final Color BG_PANEL_BUILDING   = Color.WHITE;
  public static final Color BG_HEADER_BUILDING  = new Color(44, 62, 80);
  public static final Color BG_CRITICAL = new Color(253, 237, 236);
  public static final Color BG_WARNING  = new Color(255, 248, 236);
  public static final Color BG_INFO     = new Color(232, 244, 253);
  public static final Color FG_CRITICAL = new Color(183, 28, 28);
  public static final Color FG_WARNING  = new Color(166, 77, 0);
  public static final Color FG_INFO     = new Color(13, 71, 161);
  public static final Color DIVIDER     = new Color(220, 224, 230);
  public static final Color BG_SENSOR   = new Color(248, 249, 252);
  public static final Color FG_SENSOR   = new Color(70, 85, 105);
  public static final Color BG          = Color.WHITE;
  public static final Color LABEL_COLOR = new Color(100, 110, 130);
  public static final Color VALUE_COLOR = new Color(30, 40, 60);
  public static final Color BORDER_NONE = new Color(220, 224, 230);
  public static final Color BORDER_WARN = new Color(230, 126, 34);
  public static final Color BORDER_CRIT = new Color(231, 76, 60);
  public static final Color SIM_COLOR   = new Color(142, 68, 173);
  public static final Color HINT_OK    = new Color(39, 174, 96);
  public static final Color HINT_NONE  = new Color(149, 165, 166);

  public static final DateTimeFormatter FMT =
    DateTimeFormatter.ofPattern("dd-mm-yyyy  HH:mm:ss");
}
