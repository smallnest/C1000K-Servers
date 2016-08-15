package main

import (
	"container/list"
	"encoding/json"
	"fmt"
	"math/rand"
	"os"
	"strconv"
	"sync"
	"time"

	"goserver"

	"github.com/fasthttp-contrib/websocket"
	"github.com/valyala/fasthttp"
	//"unsafe"
)

var Config goserver.Configuration

const n = 10

var wsList = [n]*list.List{}
var locks = [n]sync.RWMutex{}

func wsHandler(ws *websocket.Conn) {
	//p := unsafe.Pointer(&ws)
	//index := ((int)(uintptr(p))) % n
	index := rand.Intn(n)
	lock := locks[index]
	lock.Lock()
	wsList[index].PushBack(ws)
	lock.Unlock()

	for {

		mt, _, err := ws.ReadMessage()

		if err != nil {
			fmt.Println("Can't receive because of " + err.Error())
			break
		}

		if mt != websocket.TextMessage {
			fmt.Printf("wrong message type:  %d\n", mt)
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
	for i := 0; i < n; i++ {
		wsList[i] = list.New()
	}

	seed := time.Now().UTC().UnixNano()
	rand.Seed(seed)

	Config = load("config.json")

	delay, _ := time.ParseDuration(Config.Delay)
	interval, _ := time.ParseDuration(Config.Interval)
	timer := time.NewTimer(delay)
	if !Config.OnlyTestConnect {
		go func() {
			for {
				<-timer.C
				timer.Reset(interval)
				totalLen := 0
				for i := 0; i < n; i++ {
					totalLen += wsList[i].Len()
				}
				if totalLen >= Config.TotalSize {
					fmt.Println("all clients are ready. send timestamp to all")
					for i := 0; i < n; i++ {
						i := i
						go func() {
							for e := wsList[i].Front(); e != nil; e = e.Next() {
								var ws = e.Value.(*websocket.Conn)
								now := time.Now().UnixNano() / int64(time.Millisecond)
								err := ws.WriteMessage(websocket.TextMessage, []byte(strconv.FormatInt(now, 10)))
								if err != nil {
									panic("Error: " + err.Error())
								}
							}
						}()

					}
				} else {
					fmt.Println("current websockets: " + strconv.Itoa(totalLen))
				}

			}
		}()
	}

	var upgrader = websocket.New(wsHandler)

	err := fasthttp.ListenAndServe(":"+strconv.Itoa(Config.Port), func(ctx *fasthttp.RequestCtx) {
		err := upgrader.Upgrade(ctx)
		if err != nil {
			panic("Error: " + err.Error())
		}
	})

	if err != nil {
		panic("Error: " + err.Error())
	}
}
