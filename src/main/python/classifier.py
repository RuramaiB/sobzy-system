#!/usr/bin/env python3
import sys
import os
import json

# Add the current directory to sys.path so we can import from classifier package
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

try:
    from classifier.content_classifier import ContentClassifier
except ImportError as e:
    print(json.dumps({
        "error": True,
        "message": f"Failed to import ContentClassifier: {str(e)}",
        "category": "UNKNOWN",
        "confidence": 0.0,
        "is_allowed": True,
        "decision": "ALLOW"
    }))
    sys.exit(0)

def main():
    if len(sys.argv) < 2:
        print(json.dumps({'error': True, 'message': 'Usage: python classifier.py <url> [content]'}))
        sys.exit(1)

    url = sys.argv[1]
    content = sys.argv[2] if len(sys.argv) > 2 else ""

    try:
        classifier = ContentClassifier()
        result = classifier.classify(url, content)
        print(json.dumps(result))
    except Exception as e:
        print(json.dumps({
            "error": True,
            "message": str(e),
            "category": "UNKNOWN",
            "confidence": 0.0,
            "is_allowed": True,
            "decision": "ALLOW"
        }))

if __name__ == "__main__":
    main()
