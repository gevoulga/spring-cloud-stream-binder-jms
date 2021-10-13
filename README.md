# spring-cloud-stream-binder-jms

A binder for spring cloud stream using JMS

## Introduction

Binder using javax.jms-api 2.0.1 for the underlying binding of streams.  
Simply add your dependency of the underlying implementation of JMS (using a spring-boot-stater?), and that's it!  

## Features

- Topics & Queues supported
- Partitioning
- Delay
- Dead-Letter-Queues: by default enabled -> <topicName>.dlq 

## Examples

