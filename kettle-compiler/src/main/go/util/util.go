package util

import (
	"log"
	"os"
	"strings"
)

// Error reports a problem, including an error, and exits the program.
func Error(err error, msgs ...string) {
	s := strings.Join(msgs, " ") + ":" + err.Error()
	log.Print("protoc-gen-go: error:", s)
	os.Exit(1)
}

// Fail reports a problem and exits the program.
func Fail(msgs ...string) {
	s := strings.Join(msgs, " ")
	log.Print("protoc-gen-rxjava: error:", s)
	os.Exit(1)
}
