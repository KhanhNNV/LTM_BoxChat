
package chatapp.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AIService {
    //    public String callLangflowAPI(String prompt) {
//        try {
//            URL url = new URL("http://127.0.0.1:7860/api/v1/run/2f864847-5711-4c24-884d-84f3817c4d65"); // thay bằng Flow ID thật
//            HttpURLConnection con = (HttpURLConnection) url.openConnection();
//            con.setRequestMethod("POST");
//            con.setRequestProperty("Content-Type", "application/json");
//            con.setDoOutput(true);
//
//            String jsonInput = "{\"input\": \"" + prompt.replace("\"", "\\\"") + "\"}";
//            try (OutputStream os = con.getOutputStream()) {
//                byte[] input = jsonInput.getBytes("utf-8");
//                os.write(input, 0, input.length);
//            }
//
//            try (BufferedReader br = new BufferedReader(
//                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
//                StringBuilder response = new StringBuilder();
//                String line;
//                while ((line = br.readLine()) != null) {
//                    response.append(line.trim());
//                }
//
//                String json = response.toString();
//                return extractAiTextFromJson(json);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "Xin lỗi, tôi hiện không thể trả lời.";
//        }
//    }
//
//    private String extractAiTextFromJson(String json) {
//        // Giản lược: tìm "result": "trả lời"
//        int start = json.indexOf("\"result\":\"") + 10;
//        int end = json.indexOf("\"", start);
//        if (start >= 0 && end > start) {
//            return json.substring(start, end);
//        }
//        return "Tôi không hiểu câu hỏi.";
//    }
    public String callLangflowAPI(String prompt) {
        try {
            System.out.println(">> Prompt gửi đến Langflow: [" + prompt + "]");
            URL url = new URL("http://127.0.0.1:7860/api/v1/run/70a684b2-7740-4e24-bf7a-b4f107c62802");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);
            con.setConnectTimeout(60000);
            con.setReadTimeout(120000);

            // Tạo JSON payload đúng định dạng
            String jsonInput = String.format("{\"input_value\": \"%s\", \"input_type\": \"chat\", \"output_type\": \"chat\"}",
                    prompt.replace("\"", "\\\""));


            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Kiểm tra response code
            int responseCode = con.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Langflow API request failed with code: " + responseCode);
                return "Xin lỗi, không thể kết nối đến AI service (HTTP " + responseCode + ")";
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }
                System.out.println("Langflow raw response: " + response);

                System.out.println("Langflow raw response: " + response); // Log response
                return extractAiTextFromJson(response.toString());
            }
        } catch (Exception e) {
            System.err.println("Error calling Langflow API:");
            e.printStackTrace();
            return "Xin lỗi, tôi hiện không thể trả lời. Lỗi: " + e.getMessage();
        }
    }

    private String extractAiTextFromJson(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray outputs = root.getJSONArray("outputs");
            if (outputs.length() > 0) {
                JSONObject firstOutput = outputs.getJSONObject(0);
                JSONArray innerOutputs = firstOutput.getJSONArray("outputs");
                if (innerOutputs.length() > 0) {
                    JSONObject inside = innerOutputs.getJSONObject(0);
                    JSONObject artifacts = inside.getJSONObject("artifacts");
                    return artifacts.getString("message"); // ✅ Trả về nội dung
                }
            }
            return "Không tìm thấy phản hồi từ AI.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi khi phân tích phản hồi JSON: " + e.getMessage();
        }
    }

}
