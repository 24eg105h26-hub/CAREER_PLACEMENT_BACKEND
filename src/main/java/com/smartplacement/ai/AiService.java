package com.smartplacement.ai;

import java.util.Map;

public interface AiService {

    String generateContent(String prompt, String featureName, Long userId);

    String generateStructuredJson(String prompt, String featureName, Long userId);
}
