#!/usr/bin/env python3
"""
Test script for content classifier
Tests various URLs and validates classification results
"""

import sys
import os

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from classifier.content_classifier import ContentClassifier


def test_classifier():
    """Test the classifier with various URLs"""

    classifier = ContentClassifier()

    test_cases = [
        # Academic
        ("https://stackoverflow.com/questions/12345", "ACADEMIC"),
        ("https://github.com/user/repo", "ACADEMIC"),
        ("https://wikipedia.org/wiki/Python", "ACADEMIC"),
        ("https://coursera.org/learn/python", "ACADEMIC"),

        # Social Media
        ("https://facebook.com/profile", "SOCIAL_MEDIA"),
        ("https://twitter.com/user", "SOCIAL_MEDIA"),
        ("https://instagram.com/user", "SOCIAL_MEDIA"),

        # Gaming
        ("https://twitch.tv/stream", "GAMING"),
        ("https://steam.com/game", "GAMING"),

        # News
        ("https://cnn.com/article", "NEWS"),
        ("https://bbc.com/news", "NEWS"),

        # Entertainment
        ("https://youtube.com/watch?v=123", "ENTERTAINMENT"),
        ("https://netflix.com/watch/123", "ENTERTAINMENT"),
    ]

    print("=" * 80)
    print("Content Classifier Test Suite")
    print("=" * 80)

    passed = 0
    failed = 0

    for url, expected_category in test_cases:
        result = classifier.classify(url)

        actual_category = result['category']
        confidence = result['confidence']
        decision = result['decision']

        status = "✓ PASS" if actual_category == expected_category else "✗ FAIL"

        if actual_category == expected_category:
            passed += 1
        else:
            failed += 1

        print(f"\n{status}")
        print(f"  URL: {url}")
        print(f"  Expected: {expected_category}")
        print(f"  Actual: {actual_category} (confidence: {confidence:.2f})")
        print(f"  Decision: {decision}")

    print("\n" + "=" * 80)
    print(f"Test Results: {passed} passed, {failed} failed out of {len(test_cases)} tests")
    print("=" * 80)

    return failed == 0


if __name__ == "__main__":
    success = test_classifier()
    sys.exit(0 if success else 1)