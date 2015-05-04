package com.mixedpack.tools.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Compression {
  public static byte[] compressGZIP(String string) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream(string.length());
    GZIPOutputStream gos = new GZIPOutputStream(os);
    gos.write(string.getBytes());
    gos.close();
    byte[] compressed = os.toByteArray();
    os.close();
    return compressed;
  }

  public static String decompressGZIP(byte[] compressed) throws IOException {
    final int BUFFER_SIZE = 32;
    ByteArrayInputStream is = new ByteArrayInputStream(compressed);
    GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
    StringBuilder string = new StringBuilder();
    byte[] data = new byte[BUFFER_SIZE];
    int bytesRead;
    while ((bytesRead = gis.read(data)) != -1) {
      string.append(new String(data, 0, bytesRead));
    }
    gis.close();
    is.close();
    return string.toString();
  }

  public static byte[] compressZIP(String string, String entryName) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream(string.length());
    ZipOutputStream gos = new ZipOutputStream(os);
    ZipEntry entry = new ZipEntry(entryName);
    gos.putNextEntry(entry);
    gos.write(string.getBytes());
    gos.close();
    byte[] compressed = os.toByteArray();
    os.close();
    return compressed;
  }

  public static String decompressZIP(byte[] compressed) throws IOException {
    final int BUFFER_SIZE = 32;
    ByteArrayInputStream is = new ByteArrayInputStream(compressed);
    ZipInputStream gis = new ZipInputStream(is);
    StringBuilder string = new StringBuilder();
    byte[] data = new byte[BUFFER_SIZE];
    int bytesRead;
    while ((bytesRead = gis.read(data)) != -1) {
      string.append(new String(data, 0, bytesRead));
    }
    gis.close();
    is.close();
    return string.toString();
  }

  public static byte[] compressLZMA(String string) throws IOException {
    ByteArrayInputStream inStream = new ByteArrayInputStream(string.getBytes());
    ByteArrayOutputStream outStream = new ByteArrayOutputStream(string.length());
    SevenZip.Compression.LZMA.Encoder encoder = new SevenZip.Compression.LZMA.Encoder();
    boolean eos = false;
    encoder.SetEndMarkerMode(eos);
    encoder.WriteCoderProperties(outStream);
    long fileSize;
    if (eos)
      fileSize = -1;
    else
      fileSize = string.length();
    for (int i = 0; i < 8; i++)
      outStream.write((int) (fileSize >>> (8 * i)) & 0xFF);
    encoder.Code(inStream, outStream, -1, -1, null);
    return outStream.toByteArray();
  }

  public static String decompressLZMA(byte[] compressed) throws IOException {
    ByteArrayInputStream inStream = new ByteArrayInputStream(compressed);
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    int propertiesSize = 5;
    byte[] properties = new byte[propertiesSize];
    if (inStream.read(properties, 0, propertiesSize) != propertiesSize)
      throw new IOException("input .lzma file is too short");
    SevenZip.Compression.LZMA.Decoder decoder = new SevenZip.Compression.LZMA.Decoder();
    if (!decoder.SetDecoderProperties(properties))
      throw new IOException("Incorrect stream properties");
    long outSize = 0;
    for (int i = 0; i < 8; i++) {
      int v = inStream.read();
      if (v < 0)
        throw new IOException("Can't read stream size");
      outSize |= ((long) v) << (8 * i);
    }
    if (!decoder.Code(inStream, outStream, outSize))
      throw new IOException("Error in data stream");
    return outStream.toString();
  }
}
