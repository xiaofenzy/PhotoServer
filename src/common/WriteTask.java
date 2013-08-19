package common;

public class WriteTask {
	private String set;
	private String md5;
	private byte[] content;
	private String result;
	
	public WriteTask(String set, String md5, byte[] content) {
		this.set = set;
		this.md5 = md5;
		this.content = content;
	}
	public String getSet() {
		return set;
	}
	public void setSet(String set) {
		this.set = set;
	}
	public String getMd5() {
		return md5;
	}
	public void setMd5(String md5) {
		this.md5 = md5;
	}
	public byte[] getContent() {
		return content;
	}
	public void setContent(byte[] content) {
		this.content = content;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	
	
}
