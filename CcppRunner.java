/**
 * C/C++编译及运行
 * 
 */
public class CCppRunner {

	private String name;
	private String code;
	private String path;
	private String type;

	public CCppRunner(String name, String code, String path, String type) {
		this.name = name;
		this.code = code;
		this.path = path;
		this.type = type;
	}

	/**
	 * cmd调用gcc/g++编译
	 * 
	 * @return
	 */
	public JsonResult compile() {
		JsonResult jsonResult = new JsonResult();
		try {
			String file = createFile();
			String compiler = "gcc";
			if ("cpp".equals(type)) {
				compiler = "g++";
			}

			ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/C", compiler + " " + file + " -o " + path + name);
			Process process = processBuilder.start();
			process.waitFor();

			if (process.exitValue() != 0) {
				jsonResult.setSuccess(false);
				jsonResult.addMsg(output(process.getErrorStream()));
			}
		} catch (Exception e) {
			jsonResult.setSuccess(false);
			jsonResult.addMsg(e.getMessage());
		}
		return jsonResult;
	}

	/**
	 * 在指定目录path下创建程序源文件
	 * 
	 * @return
	 * @throws Exception
	 */
	public String createFile() throws Exception {
		File folder = new File(path);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		File file = new File(path + name + "." + type);
		file.createNewFile();
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write(code);
		fileWriter.close();
		return file.getPath();
	}

	/**
	 * 输入流输出为字符串
	 * 
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	public String output(InputStream inputStream) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(inputStream, "GBK"));
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line + System.getProperty("line.separator"));
			}
		} finally {
			br.close();
		}
		return sb.toString();
	}
}