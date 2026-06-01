"""
Conftest file for pytest configuration.
Contains shared fixtures and configuration for all tests.
"""
import pytest
import os
import shutil


@pytest.fixture
def temp_directory():
    """Create and clean up a temporary directory for testing"""
    test_dir = 'test_temp'
    os.makedirs(test_dir, exist_ok=True)
    yield test_dir
    # Cleanup
    if os.path.exists(test_dir):
        shutil.rmtree(test_dir)


@pytest.fixture
def mock_video_file(temp_directory):
    """Create a mock video file for testing"""
    video_path = os.path.join(temp_directory, 'test_video.mp4')
    # Create an empty file to simulate a video
    with open(video_path, 'w') as f:
        f.write('')
    return video_path
