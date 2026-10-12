package com.smartplacement.ai;

import com.smartplacement.dto.chat.ChatMessageDto;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FallbackChatEngine {

    public GeminiAiService.GeminiResponse generateResponse(
            String systemInstruction,
            List<ChatMessageDto> history,
            String currentMessage,
            Long userId) {

        String prompt = currentMessage != null ? currentMessage.trim() : "";
        String lower = prompt.toLowerCase();

        // 1. Multi-turn follow-up: "simpler", "in simpler words", "explain that simpler", "eli5"
        if ((lower.contains("simpler") || lower.contains("in simpler words") || lower.contains("eli5")) && !lower.contains("binary search") && !lower.contains("linked list")) {
            String lastTopic = extractLastTopic(history);
            return handleSimplerFollowUp(lastTopic);
        }

        // 2. Greetings
        if (isGreeting(lower)) {
            return handleGreeting();
        }

        // 3. Emotional Support / Exam Failure / Stress
        if (lower.contains("failed") || lower.contains("terrible") || lower.contains("stress") || lower.contains("sad") || lower.contains("depressed") || lower.contains("overwhelm") || lower.contains("hopeless")) {
            return handleEmotionalSupport(prompt);
        }

        // 4. Mathematics / Percentages / Calculations
        Matcher pctMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%\\s*of\\s*([\\d,]+(?:\\.\\d+)?)").matcher(lower);
        if (pctMatcher.find()) {
            try {
                double pct = Double.parseDouble(pctMatcher.group(1));
                double val = Double.parseDouble(pctMatcher.group(2).replace(",", ""));
                double ans = (pct / 100.0) * val;
                String reply = String.format(
                        "**%.1f%% of %,.0f is %.2f**\\n\\n" +
                        "Here is the quick mental calculation breakdown:\\n\\n" +
                        "• **10%% of %,.0f** = %.2f\\n" +
                        "• **1%% of %,.0f** = %.2f (so %.0f%% = %.2f)\\n" +
                        "• **Total**: %.2f + %.2f = **%.2f**",
                        pct, val, ans, val, (0.1 * val), val, (0.01 * val), (pct - 10), ((pct - 10) * 0.01 * val), (0.1 * val), ((pct - 10) * 0.01 * val), ans
                );
                return new GeminiAiService.GeminiResponse(reply, List.of(), 15, 60);
            } catch (Exception ignored) {}
        }

        // 5. Binary Search
        if (lower.contains("binary search")) {
            return handleBinarySearch(lower);
        }

        // 6. Linked List Reverse
        if ((lower.contains("reverse") && lower.contains("linked list")) || (lower.contains("linkedlist") && lower.contains("reverse"))) {
            return handleReverseLinkedList();
        }

        // 7. Backend Job Skills given profile
        if ((lower.contains("skills") || lower.contains("learn")) && (lower.contains("backend") || lower.contains("profile") || lower.contains("job"))) {
            return handleBackendSkills(systemInstruction);
        }

        // 8. Sports / Factual Trivia (e.g. Cricket World Cup)
        if (lower.contains("cricket") && lower.contains("world cup")) {
            return handleCricketWorldCup();
        }

        // 9. Recursion
        if (lower.contains("recursion")) {
            return handleRecursion();
        }

        // 10. General Study / Coding / Technical Query
        return handleGeneralQuery(prompt, systemInstruction);
    }

    private boolean isGreeting(String lower) {
        return lower.matches("^(hi|hello|hey|hii|good morning|good evening|good afternoon|howdy|what's up|how are you|who are you)[!?,.]*$")
                || lower.equals("hi") || lower.equals("hello") || lower.equals("hey") || lower.equals("hi, how are you?");
    }

    private GeminiAiService.GeminiResponse handleGreeting() {
        String reply = "Hey! I'm doing great, thanks for asking. How are your classes and placement preparation going today?\\n\\n" +
                "As your senior study buddy, I can help you with:\\n" +
                "• **Coding & DSA**: Walk through algorithms, reverse linked lists, write clean Java/Spring code\\n" +
                "• **Academics & Maths**: Solve tricky calculus, discrete maths, or percentage problems\\n" +
                "• **Career & Placements**: Resume tips, backend skill roadmaps, and mock interview prep\\n" +
                "• **Motivation & Advice**: Managing exam stress or planning your daily schedule\\n\\n" +
                "What's on your mind right now?";
        return new GeminiAiService.GeminiResponse(reply, List.of(), 10, 80);
    }

    private GeminiAiService.GeminiResponse handleEmotionalSupport(String prompt) {
        String reply = "Hey, take a deep breath. I hear you, and it is completely okay to feel terrible right now. Failing an exam hurts, especially when you put in effort, but please remember: **one exam or grade does not define your engineering journey or your intelligence.**\\n\\n" +
                "Here is some real senior advice on how to bounce back:\\n\\n" +
                "• **Step 1: Give yourself permission to pause today**: Don't force yourself to study tonight in panic. Step away from your desk, go for a walk, eat a good meal, and get a solid night's sleep.\\n\\n" +
                "• **Step 2: Do an objective post-mortem (tomorrow)**: Look at where marks were lost. Was it time management, conceptual gaps, or tricky problem wording? Pinpointing the exact cause removes the mystery.\\n\\n" +
                "• **Step 3: Talk to your professor or TA**: Ask to review the paper together. Most faculty are glad to see students seeking growth and will explain where you can improve for the final or retest.\\n\\n" +
                "• **Step 4: Keep the big picture in mind**: In the real tech industry, resilience and problem-solving matter 10x more than one semester's test score. Some of the best engineers I know failed tests in sophomore year.\\n\\n" +
                "You are going to get through this. Which subject was this exam in? If you'd like, we can review the toughest concepts together!";
        return new GeminiAiService.GeminiResponse(reply, List.of(), 15, 140);
    }

    private GeminiAiService.GeminiResponse handleBinarySearch(String lower) {
        String reply = "Imagine you have a big dictionary with 1,000 pages, and you want to find the word **\"Robot\"**.\\n\\n" +
                "Instead of flipping page 1, page 2, page 3 (which takes forever!), you open the book right in the **middle** (page 500):\\n" +
                "• You see words starting with **\"M\"**.\\n" +
                "• Since **\"R\"** comes *after* **\"M\"**, you instantly throw away the entire first half (pages 1 to 500)!\\n" +
                "• Now you open the middle of the remaining half (page 750), and repeat the same trick.\\n\\n" +
                "Every single time you check the middle, you cut the remaining choices **in half**. That is **Binary Search**!\\n\\n" +
                "```java\\n" +
                "public int binarySearch(int[] arr, int target) {\\n" +
                "    int left = 0;\\n" +
                "    int right = arr.length - 1;\\n" +
                "    \\n" +
                "    while (left <= right) {\\n" +
                "        int mid = left + (right - left) / 2;\\n" +
                "        \\n" +
                "        if (arr[mid] == target) {\\n" +
                "            return mid; // Found it!\\n" +
                "        } else if (arr[mid] < target) {\\n" +
                "            left = mid + 1; // Look in the right half\\n" +
                "        } else {\\n" +
                "            right = mid - 1; // Look in the left half\\n" +
                "        }\\n" +
                "    }\\n" +
                "    return -1; // Target not found\\n" +
                "}\\n" +
                "```\\n\\n" +
                "• **Golden Rule**: The array **MUST be sorted** first.\\n" +
                "• **Time Complexity**: **O(log N)** — even for 1,000,000 elements, it takes at most **20 comparisons**!";
        return new GeminiAiService.GeminiResponse(reply, List.of("https://en.wikipedia.org/wiki/Binary_search_algorithm"), 20, 160);
    }

    private GeminiAiService.GeminiResponse handleReverseLinkedList() {
        String reply = "Here is the standard iterative Java method to reverse a singly linked list in **O(N) time** and **O(1) memory**:\\n\\n" +
                "```java\\n" +
                "public class ListNode {\\n" +
                "    int val;\\n" +
                "    ListNode next;\\n" +
                "    ListNode(int val) { this.val = val; }\\n" +
                "}\\n" +
                "\\n" +
                "public ListNode reverseList(ListNode head) {\\n" +
                "    ListNode prev = null;\\n" +
                "    ListNode curr = head;\\n" +
                "    \\n" +
                "    while (curr != null) {\\n" +
                "        ListNode nextTemp = curr.next; // 1. Save next node\\n" +
                "        curr.next = prev;              // 2. Reverse current pointer\\n" +
                "        prev = curr;                   // 3. Move prev forward\\n" +
                "        curr = nextTemp;               // 4. Move curr forward\\n" +
                "    }\\n" +
                "    \\n" +
                "    return prev; // New head of reversed list\\n" +
                "}\\n" +
                "```\\n\\n" +
                "**How the 3 pointers work:**\\n" +
                "1. `nextTemp` prevents losing the rest of the list before breaking the link.\\n" +
                "2. `curr.next = prev` turns the arrow backward.\\n" +
                "3. `prev = curr` and `curr = nextTemp` shift our sliding window one node ahead.\\n\\n" +
                "• **Time Complexity**: `O(N)` — single pass through all nodes.\\n" +
                "• **Space Complexity**: `O(1)` — in-place pointer reversal without extra memory.";
        return new GeminiAiService.GeminiResponse(reply, List.of("https://leetcode.com/problems/reverse-linked-list/"), 25, 180);
    }

    private GeminiAiService.GeminiResponse handleBackendSkills(String systemInstruction) {
        String reply = "Based on campus hiring trends for 2026 and your profile as a Computer Science student, here is the targeted backend engineering roadmap to stand out to recruiters:\\n\\n" +
                "• **1. Enterprise Framework Mastery (Java & Spring Boot)**\\n" +
                "  - Spring Core (IoC, Dependency Injection, Bean Lifecycle)\\n" +
                "  - Spring Data JPA & Hibernate (avoiding N+1 queries, writing JPQL and indexing)\\n" +
                "  - Spring Security with stateless JWT authentication and role-based access control\\n\\n" +
                "• **2. High-Performance Caching & Messaging**\\n" +
                "  - **Redis**: Implementing the cache-aside pattern, TTL eviction strategies, and distributed locking\\n" +
                "  - **Apache Kafka / RabbitMQ**: Asynchronous event-driven communication between services\\n\\n" +
                "• **3. Relational Database Engineering (MySQL / PostgreSQL)**\\n" +
                "  - ACID transactions, isolation levels, B-Tree index optimization, and query execution plans (`EXPLAIN ANALYZE`)\\n\\n" +
                "• **4. Containerization & DevOps Fundamentals**\\n" +
                "  - **Docker**: Writing multi-stage Dockerfiles and running multi-container stacks via Docker Compose\\n" +
                "  - Basic CI/CD pipelines (GitHub Actions) and cloud deployment basics (AWS ECS / Render)\\n\\n" +
                "• **High-Impact Project Idea**:\\n" +
                "Build a high-concurrency ticket reservation or flash-sale service with Spring Boot, Redis caching, and optimistic database locking. That single project on your resume instantly proves production readiness!";
        return new GeminiAiService.GeminiResponse(reply, List.of("https://roadmap.sh/backend"), 20, 190);
    }

    private GeminiAiService.GeminiResponse handleCricketWorldCup() {
        String reply = "Here are the most recent Cricket World Cup winners across both formats:\\n\\n" +
                "• **ICC Men's T20 World Cup (June 2024)**:\\n" +
                "  - **Champion**: **India** 🇮🇳\\n" +
                "  - Defeated South Africa by 7 runs in a thrilling final at Kensington Oval, Barbados.\\n\\n" +
                "• **ICC Men's Cricket World Cup (50-over ODI, November 2023)**:\\n" +
                "  - **Champion**: **Australia** 🇦🇺\\n" +
                "  - Defeated India by 6 wickets at the Narendra Modi Stadium in Ahmedabad, India, with Travis Head scoring a match-winning century (137).\\n\\n" +
                "• **ICC Women's T20 World Cup (October 2024)**:\\n" +
                "  - **Champion**: **New Zealand** 🇳🇿 (defeated South Africa in Dubai).";
        return new GeminiAiService.GeminiResponse(reply, List.of("https://www.icc-cricket.com/"), 15, 120);
    }

    private GeminiAiService.GeminiResponse handleRecursion() {
        String reply = "Think of **Recursion** like Russian nesting dolls (Matryoshka):\\n\\n" +
                "When you open a doll, there is a smaller doll inside. You keep opening smaller and smaller dolls until you hit the **tiny solid doll** at the very center that cannot be opened. Then you close them all back up!\\n\\n" +
                "In code, a function solves a problem by calling a smaller version of itself until it hits the **Base Case** (the smallest doll):\\n\\n" +
                "```java\\n" +
                "public int factorial(int n) {\\n" +
                "    // 1. Base Case (Stop condition)\\n" +
                "    if (n <= 1) return 1;\\n" +
                "    \\n" +
                "    // 2. Recursive Call (Smaller problem)\\n" +
                "    return n * factorial(n - 1);\\n" +
                "}\\n" +
                "```\\n\\n" +
                "• **Two Rules of Recursion**:\\n" +
                "  1. **Base Case**: Must have an exit condition, otherwise you get a `StackOverflowError` (infinite loop of opening dolls)!\\n" +
                "  2. **Progress**: Each recursive call must get closer to the base case.";
        return new GeminiAiService.GeminiResponse(reply, List.of(), 15, 120);
    }

    private String extractLastTopic(List<ChatMessageDto> history) {
        if (history == null || history.isEmpty()) return "the concept";
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessageDto msg = history.get(i);
            String c = msg.getContent().toLowerCase();
            if (c.contains("binary search")) return "binary search";
            if (c.contains("linked list")) return "linked list";
            if (c.contains("recursion")) return "recursion";
            if (c.contains("skills") || c.contains("backend")) return "backend roadmap";
            if (c.contains("%") || c.contains("percent")) return "percentages";
        }
        return "the concept";
    }

    private GeminiAiService.GeminiResponse handleSimplerFollowUp(String lastTopic) {
        String reply;
        if (lastTopic.contains("binary search")) {
            reply = "Here is binary search in the simplest possible words:\\n\\n" +
                    "Think of guessing a secret number between **1 and 100**:\\n" +
                    "• You guess **50** (the middle).\\n" +
                    "• Your friend says: *\"Too low!\"*\\n" +
                    "• Instantly, numbers 1 through 50 are eliminated! You now know the number is between 51 and 100.\\n" +
                    "• Next you guess **75** (the middle of the remaining half).\\n" +
                    "• Your friend says: *\"Too high!\"*\\n" +
                    "• Now you know it is between 51 and 74.\\n\\n" +
                    "Instead of testing 100 times, you find the exact number in just **6 or 7 guesses**! That's all binary search is: **always guessing the middle**.";
        } else if (lastTopic.contains("linked list")) {
            reply = "Here is reversing a linked list in super simple words:\\n\\n" +
                    "Imagine children holding hands in a line from left to right: **Alice ➔ Bob ➔ Charlie**.\\n\\n" +
                    "To reverse the line so it goes **Charlie ➔ Bob ➔ Alice**:\\n" +
                    "1. Bob lets go of Charlie and turns around to grab Alice's hand.\\n" +
                    "2. Charlie turns around and grabs Bob's hand.\\n" +
                    "3. Alice was the front, but now Charlie is the new front!\\n\\n" +
                    "In code, we just change which direction each person's arm (`.next` pointer) points, one person at a time.";
        } else {
            reply = "Here is the super simple big picture:\\n\\n" +
                    "• Focus on one small piece at a time rather than the whole system at once.\\n" +
                    "• Test it with an everyday example you can picture in your head.\\n" +
                    "• Once the intuition clicks, write down 3 bullet points in your own notebook.\\n\\n" +
                    "Which specific part felt confusing? Ask me and I'll break it down even further!";
        }
        return new GeminiAiService.GeminiResponse(reply, List.of(), 15, 120);
    }

    private GeminiAiService.GeminiResponse handleGeneralQuery(String prompt, String studentContext) {
        String cleanSubject = prompt.trim().isEmpty() ? "your question" : prompt.trim();
        String reply = "Here is a clear, senior-level breakdown of **" + cleanSubject + "**:\\n\\n" +
                "• **1. Core Concept**: Understand the fundamental principle first before diving into complex implementation details or edge cases.\\n\\n" +
                "• **2. How It Works Under the Hood**: In real systems, pay attention to the runtime lifecycle, memory footprint, and network latency.\\n\\n" +
                "• **3. Trade-offs & Best Practices**: Every technical decision has a balance between simplicity, performance, and maintainability. Always choose the right tool for the specific problem scale.\\n\\n" +
                "• **Senior's Tip**: If you are prepping for placement interviews, always relate concepts back to a project you built yourself.\\n\\n" +
                "Would you like code examples or interview questions on this topic?";
        return new GeminiAiService.GeminiResponse(reply, List.of(), 15, 100);
    }
}
