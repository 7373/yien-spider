package spider.base.okHttp;

import com.google.common.io.CharStreams;
import org.jsoup.nodes.Document;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
/**
 * HttpResponseV1是爬取过程中，内存中保存网页爬取信息的一个容器，Page只在内存中存 放，用于保存一些网页信息，方便用户进行自定义网页解析之类的操作。
 *
 * @author hu
 */
public class HttpResponseV1 {
	private ByteArrayInputStream raw;


	private String contentType;
	
	private String charset;

	private int status;

	private Exception exception = null;

	private String html = null;
	private Document doc = null;

	private byte[] contentBt = null;

	private Object obj = null;

	public HttpResponseV1() {
	}

	public HttpResponseV1(byte[] contentBt) {
		this.contentBt = contentBt;
	}


	public static HttpResponseV1 createSimple(String content) {
		HttpResponseV1 response = new HttpResponseV1();
		response.setHtml(content);
		return response;
	}

	public ByteArrayInputStream getRaw() {
		return raw;
	}

	public void setRaw(ByteArrayInputStream raw) {
		this.raw = raw;
	}


	public String getContent(String charset) {
		if(charset == null) {
			return html;
		}
		try {
			return CharStreams.toString(new InputStreamReader(raw, charset));
		} catch (Exception e) {
			e.printStackTrace();
			return html;
		}
	}


	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void close() {
		if(raw != null) {
			try{
				raw.close();
			} catch(Exception ex) {
				raw = null;
			}
		}
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	public Document getDoc() {
		return doc;
	}

	public void setDoc(Document doc) {
		this.doc = doc;
	}

	public byte[] getContentBt() {
		return contentBt;
	}

	public void setContentBt(byte[] contentBt) {
		this.contentBt = contentBt;
	}

	public Object getObj() {
		return obj;
	}

	public void setObj(Object obj) {
		this.obj = obj;
	}

	@Override
	public String toString() {
		return "HttpResponseV1{" +
				"raw=" + raw +
				", contentType='" + contentType + '\'' +
				", charset='" + charset + '\'' +
				", status=" + status +
				", exception=" + exception +
				", html='" + html + '\'' +
				", doc=" + doc +
				", contentBt =" + contentBt +
				", obj=" + obj +
				'}';
	}
}
