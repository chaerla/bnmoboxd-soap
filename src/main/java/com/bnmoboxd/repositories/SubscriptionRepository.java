package com.bnmoboxd.repositories;

import com.bnmoboxd.database.Database;
import com.bnmoboxd.models.Subscription;
import com.bnmoboxd.struct.Pagination;
import com.bnmoboxd.struct.SubscriptionStatus;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionRepository {
    public List<Subscription> getSubscriptions(Filter filter, Pagination pagination) {
        try(Connection connection = Database.getConnection()) {
            List<Subscription> subscriptions = new ArrayList<>();

            StringBuilder query = new StringBuilder("SELECT * FROM subscriptions");
            List<String> params = new ArrayList<>();
            if(filter != null) {
                if(filter.getCuratorUsername() != null) {
                    query.append(String.format(" %s curator_username = ?", "WHERE"));
                    params.add(filter.getCuratorUsername());
                }
                if(filter.getSubscriberUsername() != null) {
                    query.append(String.format(" %s subscriber_username = ?", params.isEmpty() ? "WHERE" : "AND"));
                    params.add(filter.getSubscriberUsername());
                }
                if(filter.getStatus() != null) {
                    query.append(String.format(" %s status = ?", params.isEmpty() ? "WHERE" : "AND"));
                    params.add(filter.getStatus().toString());
                }
            }
            if(pagination != null) query.append(" LIMIT ? OFFSET ?");

            try(PreparedStatement statement = connection.prepareStatement(query.toString())) {
                for(int i = 0; i < params.size(); i++) {
                    statement.setString(i + 1, params.get(i));
                }
                if(pagination != null) {
                    int skip = pagination.getTake() * (pagination.getPage() - 1);
                    statement.setInt(params.size() + 1, pagination.getTake());
                    statement.setInt(params.size() + 2, skip);
                }

                try(ResultSet resultSet = statement.executeQuery()) {
                    while(resultSet.next()) {
                        String curatorUsername = resultSet.getString("curator_username");
                        String subscriberUsername = resultSet.getString("subscriber_username");
                        String status = resultSet.getString("status");

                        Subscription subscription = new Subscription(curatorUsername, subscriberUsername, SubscriptionStatus.valueOf(status));
                        subscriptions.add(subscription);
                    }
                }
                return subscriptions;
            }
        } catch(SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean addSubscription(String curatorUsername, String subscriberUsername, SubscriptionStatus status) {
        try(Connection connection = Database.getConnection()) {
            String query = "INSERT INTO subscriptions VALUES (?, ?, ?)";

            try(PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, curatorUsername);
                statement.setString(2, subscriberUsername);
                statement.setString(3, status.toString());

                // Will return 0 if a subscription already exists
                int rowCount = statement.executeUpdate();
                return rowCount > 0;
            }
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateSubscription(String curatorUsername, String subscriberUsername, SubscriptionStatus status) {
        try(Connection connection = Database.getConnection()) {
            String query = "UPDATE subscriptions SET status = ? WHERE curator_username = ? AND subscriber_username = ?";

            try(PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, status.toString());
                statement.setString(2, curatorUsername);
                statement.setString(3, subscriberUsername);

                int rowCount = statement.executeUpdate();
                return rowCount > 0;
            }
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Getter
    public static class Filter {
        public Filter(String curatorUsername, String subscriberUsername, String status) {
            this.curatorUsername = curatorUsername;
            this.subscriberUsername = subscriberUsername;
            this.status = status != null ? SubscriptionStatus.valueOf(status) : null;
        }

        public Filter(String curatorUsername, String subscriberUsername, @Nullable SubscriptionStatus status) {
            this.curatorUsername = curatorUsername;
            this.subscriberUsername = subscriberUsername;
            this.status = status;
        }

        private final String curatorUsername;
        private final String subscriberUsername;
        private final @Nullable SubscriptionStatus status;
    }
}
