package com.smartplacement;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GenerateSamplePdfs {

    @Test
    public void generateAllSamplePdfs() throws IOException {
        File dir = new File("sample_resumes");
        if (!dir.exists()) dir.mkdirs();

        // 1. Strong Resume
        createPdf(new File(dir, "strong_resume.pdf"), List.of(
                "Alex Chen",
                "alex.chen@smartplacement.ai | +1 555-0104 | linkedin.com/in/alexchen | github.com/alexchen",
                "PROFESSIONAL SUMMARY",
                "Results-oriented Full Stack Software Engineer with deep expertise in Java, Spring Boot, React, and AWS microservices architecture.",
                "EDUCATION",
                "Bachelor of Technology in Computer Science & Engineering | National Institute of Technology | CGPA: 8.8 | May 2025",
                "TECHNICAL SKILLS",
                "Languages: Java, Python, TypeScript, JavaScript, SQL",
                "Frameworks: Spring Boot, Spring MVC, React, Next.js, Node.js, Express, Hibernate",
                "Databases: PostgreSQL, MySQL, Redis, MongoDB",
                "Tools & DevOps: Docker, Kubernetes, AWS, Git, GitHub Actions, CI/CD, Linux, Postman",
                "Other: REST APIs, Microservices, System Design, Data Structures, Algorithms, Agile",
                "WORK EXPERIENCE",
                "Software Engineering Intern - Cloud Platforms | Tech Innovators (Jan 2024 - Jun 2024)",
                "- Architected high-throughput microservices using Java and Spring Boot, reducing API latency by 42% for 120,000 daily active users.",
                "- Engineered distributed caching layer with Redis, decreasing database queries by 65% and saving $4,500 monthly in infrastructure costs.",
                "- Deployed automated CI/CD pipeline using Docker and GitHub Actions, accelerating release cycles from 3 days to 45 minutes.",
                "TECHNICAL PROJECTS",
                "E-Commerce Microservices Engine (Java, Spring Boot, PostgreSQL, Docker, Redis)",
                "- Implemented distributed order processing system handling 2,500 transactions per second with 99.99% uptime.",
                "- Engineered event-driven messaging layer with Redis pub/sub, decreasing checkout workflow drop-off by 24%.",
                "Full Stack Collaborative Workspace (React, Node.js, TypeScript, PostgreSQL)",
                "- Developed interactive canvas and document editor in React, supporting real-time concurrent editing for 5,000 active teams."
        ));

        // 2. Weak Resume
        createPdf(new File(dir, "weak_resume.pdf"), List.of(
                "John Doe",
                "SKILLS",
                "Java, Python, HTML",
                "PROJECTS",
                "- Worked on a simple website with some friends from college.",
                "- Responsible for creating basic web pages and buttons.",
                "- Assisted with backend database tasks and duties included fixing bugs.",
                "- Helped with writing code for the login page."
        ));

        // 3. Scanned / Empty PDF (Zero text)
        createPdf(new File(dir, "scanned_empty.pdf"), List.of());

        System.out.println("Generated 3 sample PDFs in c:/TT/sample_resumes successfully!");
    }

    private static void createPdf(File file, List<String> lines) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            if (lines != null && !lines.isEmpty()) {
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    cs.newLineAtOffset(50, 750);
                    for (String line : lines) {
                        cs.showText(line);
                        cs.newLineAtOffset(0, -14);
                    }
                    cs.endText();
                }
            }
            doc.save(file);
        }
    }
}
