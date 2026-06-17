package com.jellystudy.service;

import com.jellystudy.dubbo.KnowledgePointDubboService;
import com.jellystudy.dubbo.QuestionDubboService;
import com.jellystudy.entity.Answer;
import com.jellystudy.entity.Comment;
import com.jellystudy.entity.KnowledgePoint;
import com.jellystudy.entity.Question;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class DataGeneratorService {

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.QuestionDubboService", timeout = 120000, check = false)
    private QuestionDubboService questionService;

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.KnowledgePointDubboService", timeout = 30000, check = false)
    private KnowledgePointDubboService knowledgePointService;

    private String[] authors = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十"};
    private String[] questionTitles = {
            "Java中ArrayList和LinkedList的区别是什么？",
            "Spring Boot自动配置原理是什么？",
            "MongoDB和MySQL的适用场景有哪些？",
            "什么是微服务架构？它有什么优缺点？",
            "Redis为什么这么快？",
            "Docker容器和虚拟机有什么区别？",
            "什么是RESTful API？设计原则有哪些？",
            "Java中的线程池是如何工作的？",
            "Git的工作流有哪些？",
            "什么是JVM内存模型？"
    };
    private String[] questionContents = {
            "最近在学习Java集合框架，想了解一下ArrayList和LinkedList在底层实现和使用场景上的区别。",
            "Spring Boot的自动配置让开发变得很方便，想深入了解一下它的实现原理。",
            "在项目选型时，不知道该选择MongoDB还是MySQL，想了解它们的适用场景。",
            "听说微服务架构很流行，想了解一下它的概念和优缺点。",
            "Redis作为缓存数据库性能非常好，想知道它为什么这么快。",
            "Docker现在很火，想了解一下容器和传统虚拟机的区别。",
            "RESTful API设计规范很重要，想学习一下相关的设计原则。",
            "Java中的线程池可以提高并发性能，想了解一下它的工作原理。",
            "团队开发中Git的使用很重要，想了解常用的工作流。",
            "JVM内存模型对于Java开发者很重要，想系统学习一下。"
    };
    private String[] answerContents = {
            "这是一个很好的问题，我来详细解答一下...",
            "根据我的经验，这个问题可以从以下几个方面分析...",
            "我之前也遇到过类似的问题，解决方案如下...",
            "让我来梳理一下这个问题的关键点...",
            "从技术角度来看，这个问题涉及到以下几个方面...",
            "结合实际项目经验，我认为应该这样处理...",
            "这个问题需要深入理解底层原理，我来解释一下...",
            "根据业界最佳实践，推荐的做法是...",
            "我来分享一下我的理解和解决方案...",
            "这个问题比较复杂，我分几点来回答..."
    };
    private String[] commentContents = {
            "感谢分享，很有帮助！",
            "学习了，谢谢！",
            "请问还有更详细的资料吗？",
            "这个解释很清楚，点赞！",
            "补充一点，我认为...",
            "同意你的观点！",
            "能不能再深入讲一下？",
            "收藏了，以后可能会用到。",
            "这个回答解决了我的疑问。",
            "非常专业，受益匪浅！"
    };
    private String[] knowledgePointNames = {
            "Java基础", "Spring Boot", "数据库", "微服务", "Redis",
            "Docker", "API设计", "并发编程", "Git", "JVM"
    };

    public void generateRandomData() {
        generateKnowledgePoints();
        generateQuestions();
    }

    public void generateKnowledgePoints() {
        List<KnowledgePoint> existing = knowledgePointService.findAll();
        if (!existing.isEmpty()) {
            return;
        }

        for (int i = 0; i < knowledgePointNames.length; i++) {
            KnowledgePoint kp = new KnowledgePoint();
            kp.setName(knowledgePointNames[i]);
            kp.setDescription("关于" + knowledgePointNames[i] + "的知识点");
            kp.setParentId(null);
            kp.setPath("/" + knowledgePointNames[i]);
            knowledgePointService.create(kp);
        }
    }

    public void generateQuestions() {
        Random random = new Random();
        List<KnowledgePoint> kps = knowledgePointService.findAll();

        for (int i = 0; i < questionTitles.length; i++) {
            Question question = new Question();
            question.setTitle(questionTitles[i]);
            question.setContent(questionContents[i]);
            question.setAuthor(authors[random.nextInt(authors.length)]);
            question.setCreateTime(new Date());
            question.setUpdateTime(new Date());

            int kpCount = random.nextInt(3) + 1;
            List<String> kpIds = new ArrayList<>();
            for (int j = 0; j < kpCount; j++) {
                KnowledgePoint kp = kps.get(random.nextInt(kps.size()));
                if (!kpIds.contains(kp.getId())) {
                    kpIds.add(kp.getId());
                }
            }
            question.setKnowledgePointIds(kpIds);
            question.setLikeCount(random.nextInt(100));
            question.setAnswerCount(0);

            Question savedQuestion = questionService.create(question);

            generateAnswers(savedQuestion.getId(), random.nextInt(5) + 1, random);
        }
    }

    private void generateAnswers(String questionId, int answerCount, Random random) {
        for (int i = 0; i < answerCount; i++) {
            Answer answer = new Answer();
            answer.setContent(answerContents[random.nextInt(answerContents.length)]);
            answer.setAuthor(authors[random.nextInt(authors.length)]);
            answer.setLikeCount(random.nextInt(50));

            questionService.addAnswer(questionId, answer);

            Question question = questionService.findById(questionId);
            for (Answer ans : question.getAnswers()) {
                if (ans.getAuthor().equals(answer.getAuthor()) && ans.getContent().equals(answer.getContent())) {
                    generateComments(questionId, ans.getId(), random.nextInt(3), random);
                    break;
                }
            }
        }
    }

    private void generateComments(String questionId, String answerId, int commentCount, Random random) {
        for (int i = 0; i < commentCount; i++) {
            Comment comment = new Comment();
            comment.setContent(commentContents[random.nextInt(commentContents.length)]);
            comment.setAuthor(authors[random.nextInt(authors.length)]);
            comment.setLikeCount(random.nextInt(20));

            questionService.addComment(questionId, answerId, comment);
        }
    }

    public long getQuestionCount() {
        return questionService.count();
    }

    public long getKnowledgePointCount() {
        return knowledgePointService.findAll().size();
    }
}
