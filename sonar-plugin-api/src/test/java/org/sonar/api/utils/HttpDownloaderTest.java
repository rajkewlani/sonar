/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.utils;

import com.google.common.base.Charsets;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.SocketConnection;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpDownloaderTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static SocketConnection socketConnection;
  private static String baseUrl;

  @BeforeClass
  public static void startServer() throws IOException {
    socketConnection = new SocketConnection(new Container() {
      public void handle(Request req, Response resp) {
        try {
          if (req.getPath().getPath().contains("/redirect/")) {
            resp.setCode(303);
            resp.add("Location", "/");
          } else {
            resp.getPrintStream().append("agent=" + req.getValues("User-Agent").get(0));
          }
        } catch (IOException e) {
        } finally {
          try {
            resp.close();
          } catch (IOException e) {
          }
        }
      }
    });
    SocketAddress address = socketConnection.connect(new InetSocketAddress(0));

    baseUrl = "http://localhost:" + ((InetSocketAddress) address).getPort();
  }

  @AfterClass
  public static void stopServer() throws IOException {
    if (null != socketConnection) {
      socketConnection.close();
    }
  }

  @Test
  public void downloadBytes() throws URISyntaxException {
    byte[] bytes = new HttpDownloader(new Settings()).readBytes(new URI(baseUrl));
    assertThat(bytes.length).isGreaterThan(10);
  }

  @Test
  public void readString() throws URISyntaxException {
    String text = new HttpDownloader(new Settings()).readString(new URI(baseUrl), Charsets.UTF_8);
    assertThat(text.length()).isGreaterThan(10);
  }

  @Test(expected = SonarException.class)
  public void failIfServerDown() throws URISyntaxException {
    // I hope that the port 1 is not used !
    new HttpDownloader(new Settings()).readBytes(new URI("http://localhost:1/unknown"));
  }

  @Test
  public void downloadToFile() throws URISyntaxException, IOException {
    File toDir = temporaryFolder.newFolder();
    File toFile = new File(toDir, "downloadToFile.txt");

    new HttpDownloader(new Settings()).download(new URI(baseUrl), toFile);
    assertThat(toFile).exists();
    assertThat(toFile.length()).isGreaterThan(10l);
  }

  @Test
  public void shouldNotCreateFileIfFailToDownload() throws Exception {
    File toDir = temporaryFolder.newFolder();
    File toFile = new File(toDir, "downloadToFile.txt");

    try {
      // I hope that the port 1 is not used !
      new HttpDownloader(new Settings()).download(new URI("http://localhost:1/unknown"), toFile);
    } catch (SonarException e) {
      assertThat(toFile).doesNotExist();
    }
  }

  @Test
  public void userAgentIsSonarVersion() throws URISyntaxException, IOException {
    Server server = mock(Server.class);
    when(server.getVersion()).thenReturn("2.2");

    InputStream stream = new HttpDownloader(server, new Settings()).openStream(new URI(baseUrl));
    Properties props = new Properties();
    props.load(stream);
    stream.close();

    assertThat(props.getProperty("agent")).isEqualTo("Sonar 2.2");
  }

  @Test
  public void followRedirect() throws URISyntaxException {
    String content = new HttpDownloader(new Settings()).readString(new URI(baseUrl + "/redirect/"), Charsets.UTF_8);
    assertThat(content).contains("agent");
  }

  @Test
  public void shouldGetDirectProxySynthesis() throws URISyntaxException {
    ProxySelector proxySelector = mock(ProxySelector.class);
    when(proxySelector.select(any(URI.class))).thenReturn(Arrays.asList(Proxy.NO_PROXY));
    assertThat(HttpDownloader.BaseHttpDownloader.getProxySynthesis(new URI("http://an_url"), proxySelector)).isEqualTo("no proxy");
  }

  @Test
  public void shouldGetProxySynthesis() throws URISyntaxException {
    ProxySelector proxySelector = mock(ProxySelector.class);
    when(proxySelector.select(any(URI.class))).thenReturn(Arrays.<Proxy> asList(new FakeProxy()));
    assertThat(HttpDownloader.BaseHttpDownloader.getProxySynthesis(new URI("http://an_url"), proxySelector)).isEqualTo("proxy: http://proxy_url:4040");
  }

  @Test
  public void supported_schemes() {
    assertThat(new HttpDownloader(new Settings()).getSupportedSchemes()).contains("http");
  }

  @Test
  public void uri_description() throws URISyntaxException {
    String description = new HttpDownloader(new Settings()).description(new URI("http://sonarsource.org"));
    assertThat(description).matches("http://sonarsource.org \\(.*\\)");
  }
}

class FakeProxy extends Proxy {
  public FakeProxy() {
    super(Type.HTTP, new InetSocketAddress("http://proxy_url", 4040));
  }
}
