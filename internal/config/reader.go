package config

import (
	"encoding/json"
	"io"
	"os"
	"path/filepath"
	"strings"

	"gopkg.in/yaml.v2"
)

type format int8

const (
	formatUnknown format = iota
	formatJSON           // Default
	formatYAML
)

var extToFormatMap = map[string]format{
	"json": formatJSON,
	"yaml": formatYAML,
	"yml":  formatYAML,
}

func extToFormat(ext string) format {
	f, ok := extToFormatMap[ext]
	if !ok {
		return formatUnknown
	}

	return f
}

func ReadFile(path string) (*Config, error) {
	ext := strings.TrimPrefix(filepath.Ext(path), ".")
	f := extToFormat(ext)

	file, err := os.OpenFile(path, os.O_RDONLY, 0755)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	c, err := decode(file, f)
	if err != nil {
		return nil, err
	}

	//sortEntries(c)
	return c, err
}

func decode(r io.Reader, f format) (*Config, error) {
	var c Config
	switch f {
	case formatYAML:
		if err := yaml.NewDecoder(r).Decode(&c); err != nil {
			return nil, err
		}
	default:
		if err := json.NewDecoder(r).Decode(&c); err != nil {
			return nil, err
		}
	}

	//sortEntries(&c)
	return &c, nil
}

//func sortEntries(c *Config) {
//	sort.Slice(c.Deployments, func(i, j int) bool {
//		return strings.Compare(c.Deployments[i].Name, c.Deployments[j].Name) < 0
//	})
//
//	sort.Strings(c.Lambdas)
//}
