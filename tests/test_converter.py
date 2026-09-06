import pytest
import os
from unittest.mock import patch, call, MagicMock
from converter import convert_file


class TestConvertFile:
    """Test cases for the convert_file function"""

    @patch('converter.subprocess.run')
    @patch('converter.os.path.splitext')
    def test_convert_file_success(self, mock_splitext, mock_run):
        """Test successful file conversion"""
        mock_splitext.return_value = ('temp/video', '.mp4')
        
        result = convert_file('temp/video.mp4', 'mp3')
        
        assert result == 'temp/video.mp3'
        mock_splitext.assert_called_once_with('temp/video.mp4')
        mock_run.assert_called_once_with(['ffmpeg', '-i', 'temp/video.mp4', 'temp/video.mp3'])

    @patch('converter.subprocess.run')
    @patch('converter.os.path.splitext')
    def test_convert_file_to_different_formats(self, mock_splitext, mock_run):
        """Test conversion to different formats"""
        test_cases = [
            ('input.mp4', 'mp3', 'input.mp3'),
            ('input.webm', 'mp4', 'input.mp4'),
            ('input.mov', 'webm', 'input.webm'),
        ]
        
        for input_file, output_format, expected_output in test_cases:
            mock_splitext.reset_mock()
            mock_run.reset_mock()
            
            base_name = os.path.splitext(input_file)[0]
            mock_splitext.return_value = (base_name, os.path.splitext(input_file)[1])
            
            result = convert_file(input_file, output_format)
            
            assert result == expected_output
            mock_run.assert_called_once_with(['ffmpeg', '-i', input_file, expected_output])

    @patch('converter.subprocess.run')
    @patch('converter.os.path.splitext')
    def test_convert_file_calls_ffmpeg_with_correct_args(self, mock_splitext, mock_run):
        """Test that ffmpeg is called with correct arguments"""
        input_file = 'temp/test_video.mp4'
        output_format = 'webm'
        
        mock_splitext.return_value = ('temp/test_video', '.mp4')
        
        convert_file(input_file, output_format)
        
        expected_call = ['ffmpeg', '-i', input_file, 'temp/test_video.webm']
        mock_run.assert_called_once_with(expected_call)

    @patch('converter.subprocess.run')
    @patch('converter.os.path.splitext')
    def test_convert_file_returns_output_filename(self, mock_splitext, mock_run):
        """Test that function returns the output filename"""
        mock_splitext.return_value = ('path/to/file', '.mp4')
        
        result = convert_file('path/to/file.mp4', 'mp3')
        
        assert result == 'path/to/file.mp3'

    @patch('converter.subprocess.run')
    @patch('converter.os.path.splitext')
    def test_convert_file_with_special_characters(self, mock_splitext, mock_run):
        """Test conversion with special characters in filename"""
        mock_splitext.return_value = ('temp/my video (2024)', '.mp4')
        
        result = convert_file('temp/my video (2024).mp4', 'mp3')
        
        assert result == 'temp/my video (2024).mp3'
        mock_run.assert_called_once_with(['ffmpeg', '-i', 'temp/my video (2024).mp4', 'temp/my video (2024).mp3'])

    @patch('converter.subprocess.run')
    @patch('converter.os.path.splitext')
    def test_convert_file_preserves_path(self, mock_splitext, mock_run):
        """Test that file path is preserved during conversion"""
        mock_splitext.return_value = ('downloads/subfolder/video', '.mov')
        
        result = convert_file('downloads/subfolder/video.mov', 'mp4')
        
        assert result == 'downloads/subfolder/video.mp4'
        mock_run.assert_called_once_with(['ffmpeg', '-i', 'downloads/subfolder/video.mov', 'downloads/subfolder/video.mp4'])
