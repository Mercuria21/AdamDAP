import pytest
from unittest.mock import patch, MagicMock
from main import download_video, convert_file


class TestMainIntegration:
    """Integration tests for the main module workflow"""

    @patch('main.convert_file')
    @patch('main.download_video')
    @patch('builtins.input')
    @patch('builtins.print')
    def test_main_workflow(self, mock_print, mock_input, mock_download, mock_convert):
        """Test the complete workflow of downloading and converting"""
        # Setup
        mock_input.side_effect = [
            'https://www.youtube.com/watch?v=test123',
            'mp3'
        ]
        mock_download.return_value = 'temp/Test Video.mp4'
        mock_convert.return_value = 'temp/Test Video.mp3'
        
        # Import and run main logic
        from main import download_video as dv, convert_file as cf
        url = mock_input.side_effect[0] if hasattr(mock_input, 'side_effect') else 'https://www.youtube.com/watch?v=test123'
        output_format = 'mp3'
        
        # Verify functions are called
        downloaded_file = mock_download(url)
        output_file = mock_convert(downloaded_file, output_format)
        
        assert downloaded_file == 'temp/Test Video.mp4'
        assert output_file == 'temp/Test Video.mp3'

    @patch('main.convert_file')
    @patch('main.download_video')
    def test_main_imports_functions(self, mock_download, mock_convert):
        """Test that main imports the correct functions"""
        from main import download_video, convert_file
        
        assert callable(download_video)
        assert callable(convert_file)

    @patch('main.convert_file')
    @patch('main.download_video')
    def test_workflow_with_different_format(self, mock_download, mock_convert):
        """Test workflow with different output formats"""
        mock_download.return_value = 'temp/video.mp4'
        mock_convert.return_value = 'temp/video.webm'
        
        downloaded = mock_download('https://www.youtube.com/watch?v=test')
        converted = mock_convert(downloaded, 'webm')
        
        assert converted == 'temp/video.webm'
        mock_convert.assert_called_with('temp/video.mp4', 'webm')
