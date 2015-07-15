package main

import (
	"container/list"
	"fmt"
	"golang.org/x/net/websocket"
	"net/http"
	"strconv"
	"time"
	"encoding/json"
	"os"
	"sync"
	"goserver"
	"math/rand"
	//"unsafe"
)

var Config goserver.Configuration
const n = 10
var wsList = [n]*list.List {list.New(),list.New(),list.New(),list.New(),list.New(),list.New(),list.New(),list.New(),list.New(),list.New()}
var locks = [n]sync.RWMutex {}


func wsHandler(ws *websocket.Conn) {
	//p := unsafe.Pointer(&ws)
	//index := ((int)(uintptr(p))) % n
	index := rand.Intn(n)
	lock := locks[index]
	lock.Lock()
	wsList[index].PushBack(ws)		
	lock.Unlock()
	
	for {
		var reply string
		if err := websocket.Message.Receive(ws, &reply); err != nil {
			fmt.Println("Can't receive because of " + err.Error())
			break
		}
	}
	
	lock.Lock()
	for e := wsList[index].Front(); e != nil; e = e.Next() {
		if e.Value.(*websocket.Conn) == ws {
			wsList[index].Remove(e)
			break
		}
	}
	lock.Unlock()
}

func load(configfile string) goserver.Configuration {
	config := goserver.Configuration{}
	file, _ := os.Open(configfile)
	decoder := json.NewDecoder(file)
	err := decoder.Decode(&config)
	if err != nil {
		panic(err.Error())
	}

	return config
}

func main() {
	seed := time.Now().UTC().UnixNano()
	rand.Seed(seed)

	Config = load("config.json")
	
	//http.Handle("/", websocket.Handler(wsHandler))
	http.HandleFunc("/", func(w http.ResponseWriter, req *http.Request) {
		s := websocket.Server{Handler: websocket.Handler(wsHandler)}
		s.ServeHTTP(w, req)
	})
	
	delay, _ := time.ParseDuration(Config.Delay)
	interval, _ := time.ParseDuration(Config.Interval)
	timer := time.NewTimer(delay)
	if !Config.OnlyTestConnect {
		go func() {
			for {
				<-timer.C
				timer.Reset(interval)
				totalLen := 0
				for i :=0; i < n; i++ {
					totalLen += wsList[i].Len()
				}
				if totalLen >= Config.TotalSize {
					fmt.Println("send timestamp to all")
					for i :=0; i < n; i++ {
						for e := wsList[i].Front(); e != nil; e = e.Next() {
							var ws = e.Value.(*websocket.Conn)
							now := time.Now().UnixNano() / int64(time.Millisecond)
							err := websocket.Message.Send(ws, strconv.FormatInt(now, 10))
							if err != nil {
								panic("Error: " + err.Error())
							}
						}
					}					
				} else {
					fmt.Println("current websockets: " + strconv.Itoa(totalLen))
				}
				
			}
		}()
	}

	err := http.ListenAndServe(":"+strconv.Itoa(Config.Port), nil)
	if err != nil {
		panic("Error: " + err.Error())
	}
}
