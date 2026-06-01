import pytest
from unittest.mock import Mock, patch, MagicMock
from downloader import download_video


class TestDownloadVideo:
    """Test cases for the download_video function"""

    @patch('downloader.yt_dlp.YoutubeDL')
    def test_download_video_success(self, mock_ydl_class):
        """Test successful video download"""
        # Setup mock
        mock_ydl = MagicMock()
        mock_ydl_class.return_value.__enter__.return_value = mock_ydl
        
        mock_info = {
            'title': 'Test Video',
            'ext': 'mp4',
            'id': 'test123'
        }
        mock_ydl.extract_info.return_value = mock_info
        mock_ydl.prepare_filename.return_value = 'temp/Test Video.mp4'
        
        # Test
        result = download_video('https://www.youtube.com/watch?v=test123')
        
        # Assert
        assert result == 'temp/Test Video.mp4'
        mock_ydl.extract_info.assert_called_once_with('https://www.youtube.com/watch?v=test123', download=True)
        mock_ydl.prepare_filename.assert_called_once_with(mock_info)

    @patch('downloader.yt_dlp.YoutubeDL')
    def test_download_video_with_custom_format(self, mock_ydl_class):
        """Test that download uses bestvideo+bestaudio format"""
        mock_ydl = MagicMock()
        mock_ydl_class.return_value.__enter__.return_value = mock_ydl
        mock_ydl.extract_info.return_value = {'title': 'Test', 'ext': 'mp4'}
        mock_ydl.prepare_filename.return_value = 'temp/Test.mp4'
        
        download_video('https://www.youtube.com/watch?v=test')
        
        # Verify YoutubeDL was initialized with correct options
        call_args = mock_ydl_class.call_args
        assert call_args is not None
        options = call_args[1] if len(call_args) > 1 else call_args[0][0]
        assert options['format'] == 'bestvideo + bestaudio/best'
        assert 'temp/' in options['outtmpl']

    @patch('downloader.yt_dlp.YoutubeDL')
    def test_download_video_returns_filename(self, mock_ydl_class):
        """Test that function returns the prepared filename"""
        mock_ydl = MagicMock()
        mock_ydl_class.return_value.__enter__.return_value = mock_ydl
        expected_filename = 'temp/My Video Title.mp4'
        
        mock_ydl.extract_info.return_value = {'title': 'My Video Title', 'ext': 'mp4'}
        mock_ydl.prepare_filename.return_value = expected_filename
        
        result = download_video('https://www.youtube.com/watch?v=abc123')
        
        assert result == expected_filename

    @patch('downloader.yt_dlp.YoutubeDL')
    def test_download_video_extracts_info(self, mock_ydl_class):
        """Test that download_video calls extract_info with download=True"""
        mock_ydl = MagicMock()
        mock_ydl_class.return_value.__enter__.return_value = mock_ydl
        mock_ydl.extract_info.return_value = {'title': 'Test', 'ext': 'mp4'}
        mock_ydl.prepare_filename.return_value = 'temp/Test.mp4'
        
        url = 'https://www.youtube.com/watch?v=test123'
        download_video(url)
        
        mock_ydl.extract_info.assert_called_once_with(url, download=True)
