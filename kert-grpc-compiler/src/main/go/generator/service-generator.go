package generator

import (
	"bytes"
	descriptor "google.golang.org/protobuf/types/descriptorpb"
	"leap.ws/kert-grpc-compiler/generator/templates"
	"path"
	"strings"
	"text/template"
	"unicode"
)

const (
	pluginVersion = "0.5"
)

type MethodType int32

const (
	Unary           MethodType = 0
	ServerStreaming MethodType = 1
	ClientStreaming MethodType = 2
	BidiStreaming   MethodType = 3
)

type Message struct {
	Name     string
	JavaName string
	Package  string
}

type Method struct {
	InputType      string
	OutputType     string
	Name           string
	JavaName       string
	MethodType     MethodType
	GrpcMethodType string
	Id             int
	IdName         string
	FieldName      string
}

type Service struct {
	ProtoFile    string
	ProtoPackage string
	ProtoName    string

	JavaPackage    string
	Name           string
	OuterClassName string
	Methods        []Method
}

type ServiceGenerator struct {
	*bytes.Buffer

	file     *descriptor.FileDescriptorProto
	service  *descriptor.ServiceDescriptorProto
	indent   string
	messages *map[string]Message
}

func (g *ServiceGenerator) GetFileName() string {
	// get package name
	pkg := getJavaPackage(g.file)
	parent := strings.Replace(pkg, ".", "/", -1)
	name := *g.service.Name + "GrpcKt.kt"

	return path.Join(parent, name)
}

// Fill the response protocol buffer with the generated output for all the files we're
// supposed to generate.
func (g *ServiceGenerator) Generate() error {
	g.Buffer = new(bytes.Buffer)

	service := g.generateTemplateParams()
	tmpl, err := template.New("service").Parse(templates.Service)
	if err != nil {
		return err
	}

	if tmpl.Execute(g.Buffer, service) != nil {
		return nil
	}

	return nil
}

func (g *ServiceGenerator) generateTemplateParams() *Service {
	methods := make([]Method, len(g.service.Method))
	for i, method := range g.service.Method {
		methodType := getMethodType(method)
		methods[i] = Method{
			Name:           method.GetName(),
			JavaName:       lowerMethodName(method),
			InputType:      g.javaClassName(method.GetInputType()),
			OutputType:     g.javaClassName(method.GetOutputType()),
			MethodType:     methodType,
			GrpcMethodType: getGrpcMethodType(methodType),
			Id:             i,
			IdName:         methodIdFieldName(method),
			FieldName:      methodPropertiesFieldName(method),
		}
	}

	service := Service{
		JavaPackage:    getJavaPackage(g.file),
		Name:           g.service.GetName(),
		ProtoFile:      g.file.GetName(),
		ProtoPackage:   g.file.GetPackage(),
		ProtoName:      g.file.GetPackage() + "." + g.service.GetName(),
		OuterClassName: getOuterClassName(g.file),
		Methods:        methods,
	}

	return &service
}

func (m *Method) FullInputType() string {
	switch m.MethodType {
	case Unary:
		fallthrough
	case ServerStreaming:
		return m.InputType
	case ClientStreaming:
		fallthrough
	case BidiStreaming:
		return "Flow<" + m.InputType + ">"
	default:
		return "UNKNOWN"
	}
}

func (m *Method) FullOutputType() string {
	switch m.MethodType {
	case Unary:
		fallthrough
	case ClientStreaming:
		return m.OutputType
	case ServerStreaming:
		fallthrough
	case BidiStreaming:
		return "Flow<" + m.OutputType + ">"
	default:
		return "UNKNOWN"
	}
}

func (m *Method) UnimplementedCall() string {
	switch m.MethodType {
	case Unary:
		fallthrough
	case ClientStreaming:
		return "unimplementedUnaryCall"
	case ServerStreaming:
		fallthrough
	case BidiStreaming:
		return "unimplementedStreamingCall"
	default:
		return "UNKNOWN"
	}
}

func (m *Method) Call() string {
	switch m.MethodType {
	case Unary:
		return "unaryCall"
	case ClientStreaming:
		return "clientStreamingCall"
	case ServerStreaming:
		return "serverStreamingCall"
	case BidiStreaming:
		return "bidiStreamingCall"
	default:
		return "UNKNOWN"
	}
}

func (m *Method) CallParams() string {
	switch m.MethodType {
	case Unary:
		fallthrough
	case ServerStreaming:
		return "req"
	case ClientStreaming:
		fallthrough
	case BidiStreaming:
		return "req"
	default:
		return "UNKNOWN"
	}
}

func getGrpcMethodType(methodType MethodType) string {
	switch methodType {
	case Unary:
		return "UNARY"
	case ServerStreaming:
		return "SERVER_STREAMING"
	case ClientStreaming:
		return "CLIENT_STREAMING"
	case BidiStreaming:
		return "BIDI_STREAMING"
	default:
		return "UNKNOWN"
	}
}

func getMethodType(method *descriptor.MethodDescriptorProto) MethodType {
	clientStreaming := method.GetClientStreaming()
	serverStreaming := method.GetServerStreaming()
	if clientStreaming {
		if serverStreaming {
			return BidiStreaming
		} else {
			return ClientStreaming
		}
	} else {
		if serverStreaming {
			return ServerStreaming
		} else {
			return Unary
		}
	}
}

// Adjust a method name prefix identifier to follow the JavaBean spec:
//   - decapitalize the first letter
//   - remove embedded underscores & capitalize the following letter
func mixedLower(word string) string {
	buffer := new(bytes.Buffer)
	buffer.WriteRune(unicode.ToLower(rune(word[0])))

	afterUnderscore := false
	for i := 1; i < len(word); i++ {
		if word[i] == '_' {
			afterUnderscore = true
		} else {
			if afterUnderscore {
				buffer.WriteRune(unicode.ToUpper(rune(word[i])))
			} else {
				buffer.WriteByte(word[i])
			}
			afterUnderscore = false
		}
	}

	return buffer.String()
}

// Converts to the identifier to the ALL_UPPER_CASE format.
//   - An underscore is inserted where a lower case letter is followed by an
//     upper case letter.
//   - All letters are converted to upper case
func toAllUpperCase(word string) string {
	buffer := new(bytes.Buffer)
	for i := 0; i < len(word); i++ {
		buffer.WriteRune(unicode.ToUpper(rune(word[i])))
		if (i < len(word)-1) && unicode.IsLower(rune(word[i])) && unicode.IsUpper(rune(word[i+1])) {
			buffer.WriteByte('_')
		}
	}
	return buffer.String()
}

func lowerMethodName(method *descriptor.MethodDescriptorProto) string {
	return mixedLower(method.GetName())
}

func methodPropertiesFieldName(method *descriptor.MethodDescriptorProto) string {
	return "METHOD_" + toAllUpperCase(method.GetName())
}

func methodIdFieldName(method *descriptor.MethodDescriptorProto) string {
	return "METHODID_" + toAllUpperCase(method.GetName())
}

func (g *ServiceGenerator) javaClassName(messageType string) string {
	return (*g.messages)[strings.Trim(messageType, ".")].JavaName
}
