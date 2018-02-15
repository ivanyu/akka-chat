# akka-chat

A web sockets chat server implemented in Akka, Akka Streams, and Akka Persistence.

__Not meant to be production-ready. Never tested for the real load. Not systematically supported.__

## Features

- A single channel, multiple users.
- Multiple sessions of a single user at the same time.
- Authentication (right now, only pre-defined users).
- Global serialization of chat events.
- Left/join notifications; request for users online.
- Request for "previous" messages in the chat.
- Double send prevention.
- Akka Persistence for the chat log.
- A very simple React frontend.

## How it works

### Glossary

- _User_ - a person who uses the chat and their account.
- _Client_ - a client application that connects to the server.
- _Session_ - an established connection between a client and the server.
- _Chat log_ - the ordered sequence of events that happened in the chat.

### Basics

#### Client-server communication

The communication between the server and client happens via web sockets in JSON format (described below).

#### Sessions

A session is an established connection between a client and the server.

A user can open multiple sessions with the chat through different clients.

The user _joins_ the chat when the first session is opened and _leaves_ when the last is closed. 

#### The chat log and event ordering

The chat log consists of events. There are the following types of events:
- A user joined of left.
- A user sent a message.

The chat supports the global order of events.
It assigns sequence numbers and timestamps to events, serving as the only source of truth about their sequence.
This prevents de-synchronization between the chat log on clients if they're implemented correctly.

### Authentication

The client sends [`authRequest`](#authRequest) message to the server with the login and password.
The server responses with [`authResponse`](#authResponse), which may be positive or negative if authentication failed.
In case of the negative response, the server closes the connection afterwards. 

__authRequest__

```json
{
  "msgType": "authRequest",
  "username": "alice",
  "password": "alice"
}
```

__authResponse__

```json
{
  "msgType": "authResponse",
  "success": true
}
```

_Implementation note:_ see `AuthenticationStage`.

### Ping-pong

When the client is authenticated, the server starts periodically send `ping` requests to it.
The client must respond with `pong` during the configurable period of time (1 minute by default),
otherwise, the connection will be closed by the server.

__ping__

```json
{
  "msgType": "ping"
}
```

__pong__

```json
{
  "msgType": "pong"
}
```

_Implementation note:_ see `PingPongStage`.

### The user joins the chat

When the client is authenticated, the chat actor is notified about it.
It assigns a global sequence number and a timestamp to the event of joining, adds it to the log and persists it.
The fact of joining is dispatched to other online users by sending `userJoinedOrLeft` to their sessions.

__userJoinedOrLeft__

```json
{
  "msgType": "userJoinedOrLeft",
  "seqN": 123,
  "username": "alice",
  "joined": true,
  "timestamp": "2018-02-14T21:46:37.897+02:00[Europe/Helsinki]"
}
```

### Request of other clients in the chat

Once the client has joined the chat, it normally requests the list of other clients with "online" indication by sending `getUsersInChat`.
The server replies with `usersInChat`.

__getUsersInChat__

```json
{
  "msgType": "getUsersInChat"
}
```

__usersInChat__

```json
{
  "msgType": "usersInChat",
  "users": [
    {"username": "bob", "online": true},
    {"username": "charlie", "online": false}
  ]
}
```

The client will be notified about subsequent joins and leaves.

### Request of elements in the chat log

The client also requests the existing chat log elements by sending `getChatLogElements`.
The server replies with `chatLogElements`.

The server always returns up to a specified (configurable) number of elements (50 by default).

This is also used for the scrolling chat log backwards with the loading of previous elements.

__getChatLogElements__

```json
{
  "msgType": "getChatLogElements"
}
```

It's possible to requests elements before an event with a particular sequence number.
To do this, an integer field `before` must be added.

__chatLogElements__

```json
{
  "msgType": "chatLogElements",
  "elements": [
    {
      "elementType": "userJoinedOrLeft",
      "seqN": 123,
      "username": "alice",
      "joined": true,
      "timestamp": "2018-02-14T21:46:37.897+02:00[Europe/Helsinki]"
    },
    {
      "elementType": "message",
      "seqN": 124,
      "username": "alice",
      "timestamp": "2018-02-14T21:46:37.897+02:00[Europe/Helsinki]",
      "text": "Hi there!"
    }
  ]
}
```

### Sending messages

To send a message to the chat, the client:
1. Generates a unique client-side ID, normally, UUID.
2. Sends `clientToServerMessage` to the server.
3. Waits for `messageAck` with the client-side ID and the assigned sequence number ("true" ID) and the assigned timestamp.
4. In the presentation, adds the timestamp to the message, replaces the client-side ID with the sequence number and moves the message
according to the sequence number.

The server acts the following way:
1. When receives `clientToServerMessage`, assigns a sequence number and a timestamp to it, adds it to the chat log and persists it.
2. Notifies other clients and other sessions of the sender user about the message by sending `serverToClientMessage` to them.
3. Acknowledges the message to the sender by replying with `messageAck`. 

__clientToServerMessage__:

```json
{
  "msgType": "clientToServerMessage",
  "clientSideId": "313888dd-5dc4-49e7-a54d-72c8d8c5eb26",
  "text": "Hi."
}
```

__serverToClientMessage__:

```json
{
  "msgType": "serverToClientMessage",
  "seqN": 123,
  "username": "alice",
  "timestamp": "2018-02-14T21:46:37.897+02:00[Europe/Helsinki]",
  "text": "Hi."
}
```

__messageAck__:

```json
{
  "msgType": "messageAck",
  "clientSideId": "313888dd-5dc4-49e7-a54d-72c8d8c5eb26",
  "seqN": 123,
  "timestamp": "2018-02-14T21:46:37.897+02:00[Europe/Helsinki]"
}
```

### The user leaves the chat

When all the user's sessions are closed, they _leaves_ the chat.
The chat notifies other users the same way as with joining—by sending `userJoinedOrLeft`—
but with `"joined": false`.

```json
{
  "msgType": "userJoinedOrLeft",
  "seqN": 123,
  "username": "alice",
  "joined": false,
  "timestamp": "2018-02-14T21:46:37.897+02:00[Europe/Helsinki]"
}
```

## License

Copyright 2018 Ivan Yurchenko

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
