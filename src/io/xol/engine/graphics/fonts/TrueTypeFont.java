package io.xol.engine.graphics.fonts;

import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TextureType;
import io.xol.engine.graphics.util.GuiDrawer;
import io.xol.engine.math.HexTools;
import io.xol.engine.misc.ColorsTools;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.awt.GraphicsEnvironment;

import io.xol.engine.math.lalgb.Vector4f;

/**
 * A TrueType font implementation originally for Slick, edited for Bobjob's
 * Engine
 * 
 * @original author James Chambers (Jimmy)
 * @original author Jeremy Adams (elias4444)
 * @original author Kevin Glass (kevglass)
 * @original author Peter Korzuszek (genail)
 * 
 * @new version edited by David Aaron Muhar (bobjob)
 */
public class TrueTypeFont
{
	// public static TrueTypeFont haettenschweiler = new
	// TrueTypeFont(Font.createFont(Font.TRUETYPE_FONT, new
	// FileInputStream("res/font/haettenschweiler.ttf")), false);
	public static TrueTypeFont smallfonts = new TrueTypeFont("res/font/smallfonts.ttf", false, 12F);
	public static TrueTypeFont arial12 = new TrueTypeFont("res/font/arial.ttf", false, 8F);
	public static TrueTypeFont haettenschweiler = new TrueTypeFont("res/font/haettenschweiler.ttf", false, 16F);

	public final static int ALIGN_LEFT = 0, ALIGN_RIGHT = 1, ALIGN_CENTER = 2;
	/** Array that holds necessary information about the font characters */
	
	//public int glTexIds[];
	public Texture2D glTextures[];
	public Glyph glyphs[];

	// private IntObject[] charArray = new IntObject[256];

	/** Map of user defined font characters (Character <-> IntObject) */
	// private HashMap<Character, IntObject> customChars = new
	// HashMap<Character, IntObject>();

	/** Boolean flag on whether AntiAliasing is enabled or not */
	private boolean antiAlias;

	/** Font's size */
	private int fontSize = 0;

	/** Font's height */
	private int fontHeight = 0;

	/** Texture used to cache the font 0-255 characters */
	// private int fontTextureID;

	/** Default font texture width */
	private int textureWidth = 512;

	/** Default font texture height */
	private int textureHeight = 512;

	/** A reference to Java's AWT Font that we create our font texture from */
	private Font font;

	/** The font metrics for our Java AWT font */
	private FontMetrics fontMetrics;

	private int correctL = 9;//, correctR = 8;
	
	TrueTypeFont()
	{
		//glTexIds = new int[256];
		//for (int i = 0; i < 256; i++)
		//	glTexIds[i] = -1;
		
		glTextures = new Texture2D[256];
		glyphs = new Glyph[65536];
	}

	public TrueTypeFont(Font font, boolean antiAlias)
	{
		this();
		this.font = font;
		this.fontSize = font.getSize() + 3;
		this.antiAlias = antiAlias;

		createSet(0);

		fontHeight -= 1;
		if (fontHeight <= 0)
			fontHeight = 1;
	}

	public TrueTypeFont(String string, boolean antiAlias, float sizeF)
	{
		this();
		try
		{
			font = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream(string));
			font = font.deriveFont(sizeF);
			System.out.println("Le gros fun bien dur " + font.getSize());
			this.fontSize = font.getSize() + 3;
			this.antiAlias = antiAlias;

			createSet(0);

			fontHeight -= 1;
			if (fontHeight <= 0)
				fontHeight = 1;
		}
		catch (FontFormatException | IOException e)
		{
			e.printStackTrace();
		}
	}

	/*public void setCorrection(boolean on)
	{
		if (on)
		{
			correctL = 2;
			//correctR = 1;
		}
		else
		{
			correctL = 0;
			//correctR = 0;
		}
	}*/

	private BufferedImage getFontImage(char ch)
	{
		// Create a temporary image to extract the character's size
		BufferedImage tempfontImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) tempfontImage.getGraphics();
		if (antiAlias == true)
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		g.setFont(font);
		fontMetrics = g.getFontMetrics();
		int charwidth = fontMetrics.charWidth(ch) + 8;

		if (charwidth <= 0)
		{
			charwidth = 7;
		}
		int charheight = fontMetrics.getHeight() + 3;
		if (charheight <= 0)
		{
			charheight = fontSize;
		}

		// Create another image holding the character we are creating
		BufferedImage fontImage;
		fontImage = new BufferedImage(charwidth, charheight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gt = (Graphics2D) fontImage.getGraphics();
		if (antiAlias == true)
		{
			gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		gt.setFont(font);

		gt.setColor(Color.WHITE);
		int charx = 3;
		int chary = 1;
		gt.drawString(String.valueOf(ch), (charx), (chary) + fontMetrics.getAscent());

		return fontImage;

	}

	private Texture2D createSet(int offset)
	{
		// If there are custom chars then I expand the font texture twice

		/*
		 * if (customCharsArray != null && customCharsArray.length > 0) {
		 * textureWidth *= 2; }
		 */

		// In any case this should be done in other way. Texture with size
		// 512x512
		// can maintain only 256 characters with resolution of 32x32. The
		// texture
		// size should be calculated dynamicaly by looking at character sizes.

		try
		{
			BufferedImage imgTemp = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = (Graphics2D) imgTemp.getGraphics();

			// WHY Y U DO DIS
			g.setColor(new Color(0, 0, 0, 0));
			g.fillRect(0, 0, textureWidth, textureHeight);

			int rowHeight = 0;
			int positionX = 0;
			int positionY = 0;

			// int customCharsLength = (customCharsArray != null) ?
			// customCharsArray.length : 0;

			for (int i = offset * 256; i < offset * 256 + 256; i++)
			{

				// get 0-255 characters and then custom characters
				char ch = (char) i;

				BufferedImage fontImage = getFontImage(ch);

				Glyph glyph = new Glyph(ch);

				glyph.width = fontImage.getWidth() + 1;
				glyph.height = fontImage.getHeight();

				if (positionX + glyph.width >= textureWidth)
				{
					positionX = 0;
					positionY += rowHeight;
					rowHeight = 0;
				}

				glyph.x = positionX;
				glyph.y = positionY;

				if (glyph.height > fontHeight)
				{
					fontHeight = glyph.height;
				}

				if (glyph.height > rowHeight)
				{
					rowHeight = glyph.height;
				}

				// Draw it here
				g.drawImage(fontImage, positionX, positionY, null);

				positionX += glyph.width;

				glyphs[i] = glyph;
				/*
				 * if (i < 256) { // standard characters charArray[i] =
				 * newIntObject; } else { // custom characters
				 * customChars.put(new Character(ch), newIntObject); }
				 */

				fontImage = null;
			}

			glTextures[offset] = loadImageIntoOpenGLTexture(offset, imgTemp);
			//glTexIds[offset] = loadImage(offset, imgTemp);

			return glTextures[offset];
			// .getTexture(font.toString(), imgTemp);

		}
		catch (Exception e)
		{
			System.err.println("Failed to create font.");
			e.printStackTrace();
		}
		return null;
	}

	private void drawQuad(float drawX, float drawY, float drawX2, float drawY2, float srcX, float srcY, float srcX2, float srcY2)
	{
		float DrawWidth = drawX2 - drawX;
		float DrawHeight = drawY2 - drawY;
		float TextureSrcX = srcX / textureWidth;
		float TextureSrcY = srcY / textureHeight;
		float SrcWidth = srcX2 - srcX;
		float SrcHeight = srcY2 - srcY;
		float RenderWidth = (SrcWidth / textureWidth);
		float RenderHeight = (SrcHeight / textureHeight);
		GuiDrawer.drawBoxWindowsSpace(drawX, drawY, drawX + DrawWidth, drawY + DrawHeight, TextureSrcX, TextureSrcY, TextureSrcX + RenderWidth, TextureSrcY + RenderHeight, -1, false, true, null);
	}

	public int getWidth(String whatchars)
	{
		int totalwidth = 0;
		Glyph glyph = null;
		int currentChar = 0;
		for (int i = 0; i < whatchars.length(); i++)
		{
			currentChar = whatchars.charAt(i);
			
			glyph = glyphs[currentChar];

			if (glyph != null)
				totalwidth += glyph.width - correctL;
		}
		return totalwidth;
	}
	
	public int getLinesHeight(String whatchars)
	{
		return getLinesHeight(whatchars, -1);
	}
	
	public int getLinesHeight(String whatchars, int clipX)
	{
		boolean clip = clipX != -1;
		int l = 1;
		int i = 1;
		char charCurrent;
		Glyph glyph;
		int totalwidth = 0;
		while (i < whatchars.length())
		{
			charCurrent = whatchars.charAt(i);

			Texture2D pageTexture = glTextures[charCurrent / 256];
			if(pageTexture == null)
			{
				//System.out.println("Uncached unicode page, generating");
				pageTexture = createSet(charCurrent / 256);
			}
			
			glyph = glyphs[charCurrent];
			
			if (glyph != null)
			{
				if (charCurrent == '#' && whatchars.length() - i - 1 >= 6 && (whatchars.toCharArray()[i + 1] != '#') && HexTools.isHexOnly(whatchars.substring(i + 1, i + 7)))
				{
					if (!(i > 1 && whatchars.toCharArray()[i - 1] == '#'))
					{
						//System.out.println("k");
						i+=6;
					}
				}
				else if (charCurrent == '\n')
				{
					totalwidth = 0;
					l++;
				}
				else
				{
					if(clip && (totalwidth + (glyph.width - correctL)) > clipX)
					{
						l++;
						totalwidth = 0;
						continue;
					}
					totalwidth += (glyph.width - correctL);
				}
				i++;
			}
		}
		return l;
	}

	public int getHeight()
	{
		return fontHeight;
	}

	public int getHeight(String HeightString)
	{
		return fontHeight;
	}

	public int getLineHeight()
	{
		return fontHeight;
	}
	
	public void drawString(float x, float y, String whatchars, float scaleX, int clipX,  float scaleY)
	{
		drawString(x, y, whatchars, scaleX, scaleY, ALIGN_LEFT, clipX, new Vector4f(1,1,1,1));
	}

	public void drawString(float x, float y, String whatchars, float scaleX, float scaleY)
	{
		drawString(x, y, whatchars, scaleX, scaleY, ALIGN_LEFT, -1, new Vector4f(1,1,1,1));
	}

	public void drawString(float x, float y, String whatchars, float scaleX, float scaleY, int clipX, Vector4f color)
	{
		drawString(x, y, whatchars, scaleX, scaleY, ALIGN_LEFT, clipX, color);
	}
	
	public void drawStringWithShadow(float x, float y, String whatchars, float scaleX, float scaleY, Vector4f color)
	{
		drawStringWithShadow(x, y, whatchars, scaleX, scaleY, -1, color);
	}
	
	public void drawStringWithShadow(float x, float y, String whatchars, float scaleX, float scaleY, int clipX, Vector4f color)
	{
		Vector4f colorDarkened = new Vector4f(color);
		colorDarkened.x*=0.2f;
		colorDarkened.y*=0.2f;
		colorDarkened.z*=0.2f;
		drawString(x+1*scaleX, y-1*scaleY, whatchars, scaleX, scaleY, ALIGN_LEFT, clipX, colorDarkened);
		drawString(x, y, whatchars, scaleX, scaleY, ALIGN_LEFT, clipX, color);
	}
	
	public void drawString(float x, float y, String whatchars, float scaleX, float scaleY, int format)
	{
		drawString(x, y, whatchars, scaleX, scaleY, format, -1, new Vector4f(1,1,1,1));
	}
	
	public void drawString(float x, float y, String whatchars, float scaleX, float scaleY, int format, int clipX, Vector4f color)
	{
		boolean clip = clipX != -1;
		
		Glyph glyph;
		// IntObject intObject = null;
		int charCurrent;

		int totalwidth = 0;
		int i = 0, d, c;
		float startY = 0;

		/*switch (format)
		{
		case ALIGN_RIGHT:
		{
			d = -1;
			//correctR
			c = correctL;

			while (i < endIndex)
			{
				if (whatchars.charAt(i) == '\n')
					startY -= fontHeight;
				i++;
			}
			break;
		}
		case ALIGN_CENTER:
		{
			for (int l = startIndex; l <= endIndex; l++)
			{
				charCurrent = whatchars.charAt(l);
				if (charCurrent == '\n')
					break;
				
				glyph = glyphs[charCurrent];
				totalwidth += glyph.width - correctL;
			}
			totalwidth /= -2;
		}
		case ALIGN_LEFT:
		default:
		{
			d = 1;
			c = correctL;
			break;
		}
		}*/
		d = 1;
		c = correctL;
		
		Vector4f colorModified = new Vector4f(color);
		
		while (i < whatchars.length())
		{
			charCurrent = whatchars.charAt(i);

			Texture2D pageTexture = glTextures[charCurrent / 256];
			if(pageTexture == null)
			{
				//System.out.println("Uncached unicode page, generating");
				pageTexture = createSet(charCurrent / 256);
			}
			
			glyph = glyphs[charCurrent];
			
			if (glyph != null)
			{
				//if (d < 0)
				//	totalwidth += (glyph.width - c) * d;
				if(clip && (totalwidth + (glyph.width - c)) > clipX / scaleX)
				{
					startY -= fontHeight * d;
					//System.out.println(fontHeight);
					totalwidth = 0;
					continue;
				}
				if (charCurrent == '#' && whatchars.length() - i - 1 >= 6 && (whatchars.toCharArray()[i + 1] != '#') &&  HexTools.isHexOnly(whatchars.substring(i + 1, i + 7)))
				{
					if (!(i > 1 && whatchars.toCharArray()[i - 1] == '#'))
					{

						String colorCode = whatchars.substring(i + 1, i + 7);
						int rgb[] = ColorsTools.hexToRGB(colorCode);
						// System.out.println("colorcode found ! - "+colorCode
						// +" rgb:"+rgb[1]);
						colorModified = new Vector4f(rgb[0] / 255.0f * color.x,rgb[1] / 255.0f * color.y, rgb[2] / 255.0f * color.z, color.w);
						i+=6;
					}
				}
				else if (charCurrent == '\n')
				{
					startY -= fontHeight * d;
					totalwidth = 0;
					/*if (format == ALIGN_CENTER)
					{
						for (int l = i + 1; l <= endIndex; l++)
						{
							charCurrent = whatchars.charAt(l);
							if (charCurrent == '\n')
								break;


							glyph = glyphs[charCurrent];

							totalwidth += glyph.width - correctL;
						}
						totalwidth /= -2;
					}
					*/
					// if center get next lines total width/2;
				}
				else
				{
					GuiDrawer.setState(pageTexture.getId(), true, true, colorModified);
					drawQuad((totalwidth + glyph.width) * scaleX + x, startY * scaleY + y, totalwidth * scaleX + x, (startY + glyph.height) * scaleY + y, glyph.x + glyph.width, glyph.y + glyph.height, glyph.x, glyph.y);
					//if (d > 0)
					totalwidth += (glyph.width - c) * d;
				}
				i++;
			}
		}
		//System.out.println(whatchars+":"+color+"x:="+x+"y:"+(totalwidth+y)+"y:"+y);
		// glEnd();
	}

	public static Texture2D loadImageIntoOpenGLTexture(int offset, BufferedImage bufferedImage)
	{
		try
		{
			short width = (short) bufferedImage.getWidth();
			short height = (short) bufferedImage.getHeight();
			// textureLoader.bpp = bufferedImage.getColorModel().hasAlpha() ?
			// (byte)32 : (byte)24;
			int bpp = (byte) bufferedImage.getColorModel().getPixelSize();
			ByteBuffer byteBuffer;
			DataBuffer db = bufferedImage.getData().getDataBuffer();
			if (db instanceof DataBufferInt)
			{
				int intI[] = ((DataBufferInt) (bufferedImage.getData().getDataBuffer())).getData();
				byte newI[] = new byte[intI.length * 4];
				for (int i = 0; i < intI.length; i++)
				{
					byte b[] = intToByteArray(intI[i]);
					int newIndex = i * 4;

					newI[newIndex] = b[1];
					newI[newIndex + 1] = b[2];
					newI[newIndex + 2] = b[3];
					newI[newIndex + 3] = b[0];
				}

				byteBuffer = ByteBuffer.allocateDirect(width * height * (bpp / 8)).order(ByteOrder.nativeOrder()).put(newI);
			}
			else
			{
				byteBuffer = ByteBuffer.allocateDirect(width * height * (bpp / 8)).order(ByteOrder.nativeOrder()).put(((DataBufferByte) (bufferedImage.getData().getDataBuffer())).getData());
			}
			byteBuffer.flip();

			//int internalFormat = GL_RGBA8, format = GL_RGBA;
			// IntBuffer textureId = BufferUtils.createIntBuffer(1);

			//int textureId;

			Texture2D texture = new Texture2D(TextureType.RGBA_8BPP);
			
			texture.uploadTextureData(width, height, byteBuffer);
			texture.setLinearFiltering(false);
			texture.setTextureWrapping(false);
			
			/*textureId = glGenTextures();
			glBindTexture(GL_TEXTURE_2D, textureId);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

			// glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);

			glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL_UNSIGNED_BYTE, byteBuffer);*/
			// GLU.gluBuild2DMipmaps(GL_TEXTURE_2D, internalFormat, width, height, format, GL_UNSIGNED_BYTE, byteBuffer);
			return texture;

		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}

		return null;
	}

	public static boolean isSupported(String fontname)
	{
		Font font[] = getFonts();
		for (int i = font.length - 1; i >= 0; i--)
		{
			if (font[i].getName().equalsIgnoreCase(fontname))
				return true;
		}
		return false;
	}

	public static Font[] getFonts()
	{
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
	}

	public static byte[] intToByteArray(int value)
	{
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
	}

	public void destroy()
	{
		// IntBuffer scratch = BufferUtils.createIntBuffer(1);
		// scratch.put(0, fontTextureID);
		// glBindTexture(GL_TEXTURE_2D, 0);
		// glDeleteTextures(scratch);
	}
}