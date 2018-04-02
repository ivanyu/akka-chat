class SignInView extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            username: '',
            password: ''
        };

        this.handleUsernameOnChange = this.handleUsernameOnChange.bind(this);
        this.handlePasswordOnChange = this.handlePasswordOnChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    handleUsernameOnChange(event) {
        this.setState({username: event.target.value});
    }

    handlePasswordOnChange(event) {
        this.setState({password: event.target.value});
    }

    handleSubmit(event) {
        event.preventDefault();
        this.props.handleSignIn(this.state.username, this.state.password);
    }

    render() {
        return(
            <div className="d-flex flex-column h-100 justify-content-center">
                <form className="w-25 align-self-center" onSubmit={this.handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="inputUsername">User name</label>
                        <input className="form-control" id="inputUsername" aria-describedby="usernameHelper" placeholder="User name"
                            value={this.state.username} onChange={this.handleUsernameOnChange}
                        ></input>
                    </div>
                    <div className="form-group">
                        <label htmlFor="inputPassword">Password</label>
                        <input type="password" className="form-control" id="inputPassword" placeholder="Password"
                            value={this.state.password}  onChange={this.handlePasswordOnChange}
                        ></input>
                    </div>
                    <button type="submit" className="btn btn-primary disabled">Sign In</button>
                </form>
            </div>
            );
    }
}


function ConnectingIndicator(props) {
    return (
        <div className="d-flex flex-column h-100 justify-content-center">
            <div className="align-self-center">Connecting...</div>
        </div>
    );
}


class ChatLog extends React.Component {
    constructor(props) {
        super(props);
    }

    componentDidUpdate() {
        const el = this.refs.chatLogThis;
        el.scrollTop = el.scrollHeight;
    }

    render() {
        console.log(this.props.chatLog);

        const chatLogElements = this.props.chatLog.map((logElement) => {
            var key = "";
            if (logElement.hasOwnProperty('seqN')) {
                key = "sn_" + logElement['seqN'].toString();
            } else {
                key = "csi_" + logElement['clientSideId'].toString();
            }

            switch (logElement['elementType']) {
                case 'userJoinedOrLeft': {
                    var joinedOrLeftStr = "";
                    if (logElement['joined'] === true) {
                        joinedOrLeftStr = "joined";
                    } else {
                        joinedOrLeftStr = "left";
                    }

                    var chatLogElementClassName = "chat-element";
                    if (logElement['username'] === this.props.username) {
                        chatLogElementClassName += " chat-element-me";
                    }

                    return (
                      <div key={key} className={chatLogElementClassName}>
                        <div className="clearfix">
                          <div className="float-left">
                            <span className="chat-element-user">{logElement.username}</span> {joinedOrLeftStr}
                          </div>
                          <div className="float-right">
                            <span className="chat-element-date">{logElement.timestamp}</span>
                          </div>
                        </div>
                      </div>
                        );
                }

                case 'message': {
                    var chatLogElementClassName = "chat-element";
                    if (logElement['username'] === this.props.username) {
                        chatLogElementClassName += " chat-element-me";
                    }

                    return (
                      <div key={key} className={chatLogElementClassName}>
                        <div className="clearfix">
                          <div className="float-left">
                            <span className="chat-element-user">{logElement.username}</span>
                          </div>
                          <div className="float-right">
                            <span className="chat-element-date">{logElement.timestamp}</span>
                          </div>
                        </div>
                        <div>{logElement.text}</div>
                      </div>
                        );
                }

                case 'unackedMessage': {
                    return (
                      <div key={key} className="chat-element chat-element-me">
                        <div className="clearfix">
                          <div className="float-left">
                            <span className="chat-element-user">{this.props.username}</span>
                          </div>
                          <div className="float-right">
                            <span className="chat-element-mark oi oi-clock"></span> 
                          </div>
                        </div>
                        <div>{logElement.text}</div>
                      </div>
                        );
                }

                default: {
                    return <div key={key}>{JSON.stringify(logElement)}</div>
                }
            }
        });

        return(
            <div ref="chatLogThis" className="chat-log d-flex flex-column align-items-stretch /*justify-content-end*/" style={{flex:1}}>
                {chatLogElements}
            </div>
            );
    }
}


class ChatInput extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            text: ''
        };

        this.handleTextOnChange = this.handleTextOnChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    handleTextOnChange(event) {
        this.setState({text: event.target.value});
    }

    handleSubmit(event) {
        event.preventDefault();

        const textToSend = this.state.text;

        if (textToSend) {
            this.props.handleSendMessage(textToSend);
            this.setState({text: ''});
        }
    }

    render() {
        return(
            <div className="chat-input mt-auto" style={{height: '3em'}}>
              <form className="form-inline h-100 px-1 justify-content-around align-items-center" onSubmit={this.handleSubmit}>
                <input className="form-control mx-1" style={{flex:1}}
                  value={this.state.text}  onChange={this.handleTextOnChange}
                />
                <button type="submit" className="btn btn-success"><span className="oi oi-location"></span></button>
              </form>
            </div>
            );
    }
}


class ChatUsers extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        if (this.props.usersInChat !== null) {
            const usersItems = this.props.usersInChat.map((user) => {
                var usernameStr = user['username'];
                if (user['username'] == this.props.username) {
                    usernameStr += " (you)"
                }

                var usernameStrClassnames = "oi oi-media-record ";
                if (user['online'] === true) {
                    usernameStrClassnames += "user-dot-online";
                } else {
                    usernameStrClassnames += "user-dot-offline";
                }

                return (<li className="list-group-item clearfix" key={user.username}>
                  <div className="float-left">
                    <span className={usernameStrClassnames}></span> {usernameStr}
                  </div>
                  <div className="float-right user-typing-label h-100 invisible">
                    <span style={{verticalAlign: 'middle'}}>Typing...</span>
                  </div>
                </li>);
                });

            return(
                <div className="chat-users col col-xl-3">
                    <ul className="list-group">{usersItems}</ul>
                </div>
            );
        } else {
            return <div className="chat-users col col-xl-3"></div>;
        }
    }
}


class ChatView extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className="row h-100">
                <div className="chat-log-and-input col col-xl-9 d-flex flex-column align-items-stretch">
                    <ChatLog
                      username={this.props.username}
                      chatLog={this.props.chatLog}
                    />
                    <ChatInput
                      handleSendMessage={this.props.handleSendMessage}
                    />
                </div>
                <ChatUsers
                  username={this.props.username}
                  usersInChat={this.props.usersInChat}
                />
            </div>
            );
    }
}


class App extends React.Component {
    constructor(props) {
        super(props);
        this.fsmStateNotConnected = "notConnected";
        this.fsmStateNotAuthenticated = "notAuthenticated";
        this.fsmStateAuthRequestSent = "authRequestSent";
        this.fsmStateAuthenticated = "authenticated";

        this.state = {
            fsmState: this.fsmStateNotConnected,
            username: null,
            usersInChat: null,
            chatLog: []
        };

        this.handleSignIn = this.handleSignIn.bind(this);
        this.connectAndAuthenticate = this.connectAndAuthenticate.bind(this);
        this.handleSendMessage = this.handleSendMessage.bind(this);
        this.addUnackedMessageToLog = this.addUnackedMessageToLog.bind(this);
        this.replaceUnackedMessageInLog = this.replaceUnackedMessageInLog.bind(this);
        this.addUserJoinedOrLeftToLog = this.addUserJoinedOrLeftToLog.bind(this);
        this.addAckedMessageToLog = this.addAckedMessageToLog.bind(this);
        this.insertBySeqN = this.insertBySeqN.bind(this);
    }

    handleSignIn(username, password) {
        this.setState({"username": username});
        this.connectAndAuthenticate(username, password);
    }

    connectAndAuthenticate(username, password) {
        this.wsChat = new WebSocket(appConfig["connectionString"]);

        this.wsChat.onopen = function(event) {
            console.log("connected");
            this.setState({fsmState: this.fsmStateNotAuthenticated});

            const authRequest = {
                "msgType": "authRequest",
                "username": username,
                "password": password
            };
            this.wsChat.send(JSON.stringify(authRequest));
            this.setState({
                fsmState: this.fsmStateAuthRequestSent
            });
        }.bind(this);

        this.wsChat.onmessage = this.onMessage.bind(this);
        this.wsChat.onclose = function() {
            connect();
        }.bind(this);
    }

    handleSendMessage(text) {
        //console.log("Sending " + text);

        var d = new Date().getTime();
        var clientSideId =
            'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = (d + Math.random()*16)%16 | 0;
                d = Math.floor(d/16);
                return (c=='x' ? r : (r&0x3|0x8)).toString(16);
            });

        const clientToServerMessage = {
            "msgType": "clientToServerMessage",
            "clientSideId": clientSideId,
            "text": text
        };
        this.addUnackedMessageToLog(clientSideId, text);
        this.wsChat.send(JSON.stringify(clientToServerMessage));
    }

    addUnackedMessageToLog(clientSideId, text) {
        const newChatLog = this.state.chatLog.slice();
        newChatLog.push({
            "elementType": "unackedMessage",
            "clientSideId": clientSideId,
            "text": text
        });
        this.setState({ chatLog: newChatLog });
    }

    replaceUnackedMessageInLog(clientSideId, seqN, timestamp) {
        const newChatLog = this.state.chatLog.slice();

        var text = null;
        for (var i in newChatLog) {
            if (newChatLog[i]['clientSideId'] === clientSideId) {
                text = newChatLog[i]['text'];
                newChatLog.splice(i, 1);
                break;
            }
        }

        const newElement = {
            elementType: "message",
            seqN: seqN,
            username: this.state["username"],
            timestamp: timestamp,
            text: text
        };
        this.insertBySeqN(newChatLog, newElement);
        this.setState({ chatLog: newChatLog });
    }

    addAckedMessageToLog(seqN, username, timestamp, text) {
        const newChatLog = this.state.chatLog.slice();
        const newElement = {
            elementType: "message",
            seqN: seqN,
            username: username,
            timestamp: timestamp,
            text: text
        };
        this.insertBySeqN(newChatLog, newElement);
        this.setState({ chatLog: newChatLog });
    }

    addUserJoinedOrLeftToLog(seqN, joined, username, timestamp) {
        const newChatLog = this.state.chatLog.slice();
        const newElement = {
            elementType: "userJoinedOrLeft",
            seqN: seqN,
            joined: joined,
            username: username,
            timestamp: timestamp,
        };
        this.insertBySeqN(newChatLog, newElement);
        this.setState({ chatLog: newChatLog });
    }

    insertBySeqN(arr, element) {
        var found = false;
        for (var i = arr.length - 1; i >= 0; i--) {
            if (!arr[i].hasOwnProperty('seqN')) {
                continue;
            }

            if (arr[i]['seqN'] < element['seqN']) {
                arr.splice(i + 1, 0, element);
                found = true;
                break;
            }
        }
        if (!found) {
            arr.splice(0, 0, element);
        }
    }

    onMessage(event) {
        const fsmState = this.state.fsmState;
        
        const msg = JSON.parse(event.data);
        console.log(msg);

        switch (fsmState) {
            case this.fsmStateNotConnected:
            case this.fsmStateNotAuthenticated: {
                console.error("wtf");
                return;
            }

            case this.fsmStateAuthRequestSent: {
                if (msg["msgType"] !== "authResponse") {
                    console.error("wtf");
                    return;
                }

                if (msg["success"] === true) {
                    this.setState({fsmState: this.fsmStateAuthenticated});

                    const getUsersInChat = {
                        "msgType": "getUsersInChat"
                    };
                    this.wsChat.send(JSON.stringify(getUsersInChat));

                    const getChatLogElements = {
                        "msgType": "getChatLogElements"
                    };
                    this.wsChat.send(JSON.stringify(getChatLogElements));
                } else {
                    // TODO proper error indication
                    this.setState({fsmState: this.fsmStateNotAuthenticated});
                }
                return;
            }

            case this.fsmStateAuthenticated: {
                switch (msg["msgType"]) {
                    case "usersInChat": {
                        this.setState({usersInChat: msg['users']});
                        return;
                    }

                    case "chatLogElements": {
                        this.setState({chatLog: msg['elements']});
                        return;
                    }

                    case "userJoinedOrLeft": {
                        const usersInChat = this.state['usersInChat'];
                        const newUsersInChat = [];
                        for (var i in usersInChat) {
                            const user = Object.assign({}, usersInChat[i]);
                            if (user['username'] === msg['username']) {
                                user['online'] = msg['joined'];
                            }
                            newUsersInChat.push(user);
                        }
                        this.setState({usersInChat: newUsersInChat});

                        this.addUserJoinedOrLeftToLog(
                            msg['seqN'], msg['joined'], msg['username'], msg['timestamp']);
                        return;
                    }

                    case "messageAck": {
                        this.replaceUnackedMessageInLog(msg['clientSideId'], msg['seqN'], msg['timestamp']);
                        return;
                    }

                    case "serverToClientMessage": {
                        this.addAckedMessageToLog(msg['seqN'], msg['username'], msg['timestamp'], msg['text']);
                        return;
                    }
                }

                return;
            }
        }
    }

    render() {
        const fsmState = this.state.fsmState;

        switch (fsmState) {
            case this.fsmStateNotConnected: {
                return(
                    <SignInView
                      username={this.state.username}
                      handleSignIn={this.handleSignIn}
                    />
                );
            }

            case this.fsmStateNotAuthenticated:
            case this.fsmStateAuthRequestSent: {
                return <ConnectingIndicator />
            }

            case this.fsmStateAuthenticated: {
                return <ChatView
                         username={this.state.username}
                         usersInChat={this.state.usersInChat}
                         chatLog={this.state.chatLog}
                         handleSendMessage={this.handleSendMessage}
                       />
            }
        }

    }
}

ReactDOM.render(
    <App />,
    document.getElementById('root')
);
