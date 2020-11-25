package main

import (
	"./generator"
	"./util"
	"github.com/golang/protobuf/proto"
	"io/ioutil"
	"os"
)

func main() {
	// Begin by allocating a generator. The request and response structures are stored there
	// so we can do error handling easily - the response structure contains the field to
	// report failure.
	g := generator.New()

	var data []byte = nil
	var err error = nil

	if len(os.Args) > 1 {
		filename := os.Args[1]
		if data, err = ioutil.ReadFile(filename); err != nil {
			util.Error(err, "reading input from file")
		}
	} else {
		if data, err = ioutil.ReadAll(os.Stdin); err != nil {
			util.Error(err, "reading input")
		}
	}

	if err := proto.Unmarshal(data, g.Request); err != nil {
		util.Error(err, "parsing input proto")
	}

	if len(g.Request.FileToGenerate) == 0 {
		util.Fail("no files to generate")
	}

	g.CommandLineParameters(g.Request.GetParameter())

	if err = g.WriteInput(data); err != nil {
		util.Error(err, "failed to write input data")
	}

	if err = g.GenerateAllFiles(); err != nil {
		util.Error(err, "failed to generate files")
	}

	// Send back the results.
	data, err = proto.Marshal(g.Response)
	if err != nil {
		util.Error(err, "failed to marshal output proto")
	}
	_, err = os.Stdout.Write(data)
	if err != nil {
		util.Error(err, "failed to write output proto")
	}
}
