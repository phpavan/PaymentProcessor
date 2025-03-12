package com.example;

import java.io.Serializable;

public class Customer implements Serializable {
        private static final long serialVersionUID = 1L;
		private int id;
        private String name;
        private int riskScore;

        public Customer(int id, String name, int riskScore) {
            this.id = id;
            this.name = name;
            this.riskScore = riskScore;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public int getRiskScore() { return riskScore; }
    }