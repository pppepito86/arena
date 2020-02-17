package com.olimpiici.arena.service;

import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.domain.UserPoints;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.service.mapper.CompetitionMapper;
import com.olimpiici.arena.service.mapper.CompetitionProblemMapper;
import com.olimpiici.arena.service.mapper.ProblemMapper;
import com.olimpiici.arena.service.util.IntUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.ZonedDateTime;

public class Standings {
    private UserRepository userRepository;

    private Map<Long, Map<Long, Integer>> userToPointsPerProblem 
        = new HashMap<Long, Map<Long, Integer>>();

    public Standings(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void processSubmission(Submission submission) {
        Long userId = submission.getUser().getId();
        if (!userToPointsPerProblem.containsKey(userId)) 
            userToPointsPerProblem.put(userId, new HashMap<Long, Integer>());
        Map<Long, Integer> pointsPerProblem = userToPointsPerProblem.get(userId);
        Long problem = submission.getCompetitionProblem().getId();
        Integer points = pointsPerProblem.getOrDefault(problem, 0);
        points = IntUtil.safeMax(points, submission.getPoints());
        pointsPerProblem.put(problem, points);
    }

    public List<UserPoints> getStandings() {
	    Map<Long, User> idToUser = new HashMap<>();
		userRepository
			.findAll()
			.stream()
			.forEach(user -> idToUser.put(user.getId(), user));
		
	    List<UserPoints> standings = userToPointsPerProblem
			.entrySet()
			.stream()
			.map(entry -> {
				Integer points = entry.getValue()
						.values()
						.stream()
						.mapToInt(Integer::intValue)
						.sum();
				User user = idToUser.get(entry.getKey());
				return new UserPoints(user, points);
			})
			.filter(userPoints -> userPoints.user.getId() > 4)
			.collect(Collectors.toList());
        
        Collections.sort(standings);
        return standings;
    }

    // Executes this -= other
    // For example, if "this" are standings generated at timestamp T1
    // and "old" is generated at timestamp T2 and T1 > T2 this method 
    // will return the "new points" earned between T2 and T1.
    public void minus(Standings old) {
        for (Long userId : userToPointsPerProblem.keySet()) {
            if (!old.userToPointsPerProblem.containsKey(userId)) {
                continue;
            }
            Map<Long, Integer> problemToPoints = userToPointsPerProblem.get(userId);
            Map<Long, Integer> oldProblemToPoints = old.userToPointsPerProblem.get(userId);
            for (Long problemId : problemToPoints.keySet()) {
                Integer newPoints = problemToPoints.get(problemId) - 
                    oldProblemToPoints.getOrDefault(problemId, 0);
                problemToPoints.put(problemId, newPoints);
            }
        }
    } 
}
	
