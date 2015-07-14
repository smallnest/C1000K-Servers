package main

import (
	"fmt"
	"strconv" 
	"container/list"
	"golang.org/x/net/websocket"
	"net/http"
	"time"
	"goserver"
)

var wsList = list.New()

func wsHandler(ws *websocket.Conn) {
	wsList.PushBack(ws)
	for {
        var reply string
        if err := websocket.Message.Receive(ws, &reply); err != nil {
            fmt.Println("Can't receive because of " + err.Error())
			break
        }
    }
}

func main() {
	//http.Handle("/", websocket.Handler(wsHandler))
	http.HandleFunc("/", func (w http.ResponseWriter, req *http.Request) {
        s := websocket.Server{Handler: websocket.Handler(wsHandler)}
        s.ServeHTTP(w, req)
    });
	timer := time.NewTimer(time.Minute * goserver.Delay)
	go func() {
		for {
			<-timer.C
			timer.Reset(time.Minute * goserver.Interval)
			fmt.Println("send timestamp to all")
			for e := wsList.Front(); e != nil; e = e.Next() {
				var ws = e.Value.(*websocket.Conn)
				now := time.Now().UnixNano() / int64(time.Millisecond)
				err := websocket.Message.Send(ws, strconv.FormatInt(now, 10))
				if err != nil {
					panic("Error: " + err.Error())
				}
			}
		}
	}()

	err := http.ListenAndServe(":"+ strconv.Itoa(goserver.Port), nil)
	if err != nil {
		panic("Error: " + err.Error())
	}
}
