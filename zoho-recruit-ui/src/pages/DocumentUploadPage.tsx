import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  Container,
  Box,
  TextField,
  Button,
  Typography,
  Alert,
  CircularProgress,
  Stack,
  Card,
  CardContent,
  Divider,
  Chip,
  LinearProgress,
  Fade,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import type { RootState, AppDispatch } from '../store/store';
import { setText, setPdfFile, setLoading, setError, setSuccess, setCandidates, resetForm } from '../store/documentSlice';
import { candidateService } from '../services/candidateService';
import { config } from '../config/environment';
import CandidateResultsTable from '../components/CandidateResultsTable';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import ClearIcon from '@mui/icons-material/Clear';
import SearchIcon from '@mui/icons-material/Search';

// Styled Components
const GradientCard = styled(Card)(({ theme }) => ({
  background: `linear-gradient(135deg, ${theme.palette.primary.main}15 0%, ${theme.palette.secondary.main}15 100%)`,
  border: `1px solid ${theme.palette.primary.main}30`,
  boxShadow: theme.palette.mode === 'light'
    ? '0 8px 32px rgba(63, 81, 181, 0.1)'
    : '0 8px 32px rgba(99, 125, 234, 0.2)',
  backdropFilter: 'blur(10px)',
  transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
  '&:hover': {
    boxShadow: theme.palette.mode === 'light'
      ? '0 12px 48px rgba(63, 81, 181, 0.15)'
      : '0 12px 48px rgba(99, 125, 234, 0.3)',
    transform: 'translateY(-4px)',
  },
}));

const StyledTextField = styled(TextField)({
  '& .MuiOutlinedInput-root': {
    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
    fontSize: '1rem',
    lineHeight: 1.6,
    
    '&:hover': {
      boxShadow: `0 4px 12px rgba(63, 81, 181, 0.08)`,
    },
    
    '&.Mui-focused': {
      boxShadow: `0 8px 24px rgba(63, 81, 181, 0.15)`,
    },
  },
  '& .MuiOutlinedInput-input': {
    fontFamily: '"Segoe UI", Tahoma, Geneva, Verdana, sans-serif',
    resize: 'vertical',
  },
});

const ActionButton = styled(Button)(({ theme }) => ({
  transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
  fontSize: '1rem',
  fontWeight: 600,
  textTransform: 'none',
  letterSpacing: '0.5px',
  
  '&.MuiButton-contained': {
    background: `linear-gradient(135deg, ${theme.palette.primary.main} 0%, ${theme.palette.primary.dark} 100%)`,
    boxShadow: `0 4px 15px rgba(63, 81, 181, 0.3)`,
    
    '&:hover:not(:disabled)': {
      boxShadow: `0 8px 25px rgba(63, 81, 181, 0.4)`,
      transform: 'translateY(-2px)',
    },
    
    '&:active:not(:disabled)': {
      transform: 'translateY(0)',
    },
  },
  
  '&.MuiButton-outlined': {
    borderWidth: '2px',
    
    '&:hover:not(:disabled)': {
      borderWidth: '2px',
      boxShadow: `0 4px 12px rgba(63, 81, 181, 0.15)`,
      transform: 'translateY(-2px)',
    },
  },
}));

const InfoChip = styled(Chip)(({ theme }) => ({
  margin: theme.spacing(1),
  background: `linear-gradient(135deg, ${theme.palette.success.main}20 0%, ${theme.palette.success.main}10 100%)`,
  color: theme.palette.success.dark,
  border: `1.5px solid ${theme.palette.success.main}40`,
  fontSize: '0.95rem',
  fontWeight: 500,
  
  '& .MuiChip-icon': {
    color: theme.palette.success.main,
  },
}));


const DocumentUploadPage: React.FC = () => {
  const dispatch = useDispatch<AppDispatch>();
  const { text, pdfFile, loading, error, success, candidates } = useSelector((state: RootState) => state.document);
  const [fileError, setFileError] = useState<string | null>(null);

  const handleTextChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    dispatch(setText(event.target.value));
  };

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      if (file.type === 'application/pdf') {
        dispatch(setPdfFile(file));
        setFileError(null);
      } else {
        setFileError('Only PDF files are supported');
        dispatch(setPdfFile(null));
      }
    }
  };

  const handleSubmit = async () => {
    if (!text && !pdfFile) {
      dispatch(setError('Please provide either a Job Description or upload a PDF'));
      return;
    }

    dispatch(setLoading(true));
    dispatch(setError(null));

    try {
      let candidateResults;

      if (text && !pdfFile) {
        // Only text provided - send as JSON payload
        candidateResults = await candidateService.searchByText(text);
      } else if (pdfFile) {
        // PDF provided (with or without text)
        candidateResults = await candidateService.searchByDocument(pdfFile, text);
      } else {
        throw new Error('No input provided');
      }

      dispatch(setCandidates(candidateResults));
      dispatch(setSuccess(true));
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred';
      dispatch(setError(errorMessage));
    } finally {
      dispatch(setLoading(false));
    }
  };

  const handleReset = () => {
    dispatch(resetForm());
    setFileError(null);
  };

  const hasInput = text.trim().length > 0 || pdfFile !== null;
  const textCharCount = text.length;

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: (theme) => theme.palette.mode === 'light'
          ? 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)'
          : 'linear-gradient(135deg, #0f1c27 0%, #184e5a 100%)',
        py: 8,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <Container maxWidth="lg">
        <Fade in timeout={600}>
          <Box>
            {/* Header Section */}
            <Box sx={{ textAlign: 'center', mb: 6 }}>
              <Typography
                variant="h2"
                component="h1"
                sx={{
                  fontWeight: 800,
                  background: (theme) => `linear-gradient(135deg, ${theme.palette.primary.main} 0%, ${theme.palette.primary.dark} 100%)`,
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  backgroundClip: 'text',
                  mb: 2,
                  fontSize: { xs: '2rem', sm: '2.5rem', md: '3.5rem' },
                }}
              >
                🔍 {config.appName}
              </Typography>
              <Typography
                variant="h6"
                sx={{
                  color: 'text.secondary',
                  fontWeight: 400,
                  maxWidth: '600px',
                  mx: 'auto',
                }}
              >
                Search Wissen's Zoho Recruit ATS for the best profiles given a Job Description
              </Typography>
            </Box>

            <Stack spacing={4}>
              <GradientCard elevation={0}>
                <CardContent sx={{ p: { xs: 3, sm: 4, md: 5 } }}>
                  <Stack spacing={4}>
                    {/* Status Bar */}
                    {loading && (
                      <Box sx={{ width: '100%' }}>
                        <LinearProgress
                          sx={{
                            height: 6,
                            borderRadius: 3,
                            background: 'rgba(63, 81, 181, 0.1)',
                            '& .MuiLinearProgress-bar': {
                              borderRadius: 3,
                              background: (theme) => `linear-gradient(90deg, ${theme.palette.primary.main}, ${theme.palette.primary.light})`,
                            },
                          }}
                        />
                        <Typography
                          variant="body2"
                          sx={{ mt: 1, color: 'primary.main', fontWeight: 500 }}
                        >
                          Processing your document...
                        </Typography>
                      </Box>
                    )}

                    {/* Text Input Section */}
                    <Box>
                      <Box sx={{ display: 'flex', direction: 'row', alignItems: 'center', gap: 1, mb: 2 }}>
                        <Typography
                          variant="h6"
                          sx={{
                            fontWeight: 700,
                            fontSize: '1.1rem',
                            color: 'text.primary',
                          }}
                        >
                          � Paste Job Description
                        </Typography>
                        {textCharCount > 0 && (
                          <Chip
                            label={`${textCharCount} characters`}
                            size="small"
                            variant="outlined"
                            sx={{ fontWeight: 500 }}
                          />
                        )}
                      </Box>
                      <StyledTextField
                        fullWidth
                        multiline
                        rows={7}
                        placeholder="Paste the job description here... Include skills, experience, qualifications, and requirements"
                        value={text}
                        onChange={handleTextChange}
                        variant="outlined"
                        disabled={loading}
                        slotProps={{
                          htmlInput: {
                            maxLength: config.maxTextLength,
                          },
                        }}
                      />
                      <Typography
                        variant="caption"
                        sx={{
                          display: 'block',
                          mt: 1,
                          color: 'text.secondary',
                          fontWeight: 500,
                        }}
                      >
                        {config.maxTextLength - textCharCount} characters remaining
                      </Typography>
                    </Box>

                    {/* Divider */}
                    <Box sx={{ position: 'relative', py: 1 }}>
                      <Divider sx={{ position: 'relative' }}>
                        <Chip
                          label="OR"
                          size="small"
                          sx={{
                            fontWeight: 700,
                            fontSize: '0.85rem',
                            background: (theme) => `linear-gradient(135deg, ${theme.palette.primary.main} 0%, ${theme.palette.primary.dark} 100%)`,
                            color: 'white',
                            boxShadow: '0 4px 12px rgba(63, 81, 181, 0.3)',
                          }}
                        />
                      </Divider>
                    </Box>

                    {/* PDF Upload Section */}
                    <Box>
                      <Box sx={{ display: 'flex', direction: 'row', alignItems: 'center', gap: 2, mb: 2 }}>
                        <Typography
                          variant="h6"
                          sx={{
                            fontWeight: 700,
                            fontSize: '1.1rem',
                            color: 'text.primary',
                          }}
                        >
                          📄 Upload Job Description (Optional)
                        </Typography>
                        {pdfFile && (
                          <InfoChip
                            icon={<CheckCircleIcon />}
                            label={`${(pdfFile.size / 1024).toFixed(1)}KB`}
                            size="small"
                          />
                        )}
                      </Box>

                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <input
                          type="file"
                          accept=".pdf"
                          onChange={handleFileChange}
                          style={{ display: 'none' }}
                          id="pdf-input"
                          disabled={loading}
                        />
                        <label htmlFor="pdf-input">
                          <Button
                            component="span"
                            variant="outlined"
                            startIcon={<FileDownloadIcon />}
                            disabled={loading}
                            sx={{
                              textTransform: 'none',
                              fontWeight: 600,
                              borderRadius: 1,
                            }}
                          >
                            Choose PDF
                          </Button>
                        </label>

                        {pdfFile && (
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Chip
                              label={pdfFile.name}
                              onDelete={() => {
                                dispatch(setPdfFile(null));
                                setFileError(null);
                              }}
                              icon={<CheckCircleIcon />}
                              sx={{ maxWidth: '250px' }}
                            />
                          </Box>
                        )}
                      </Box>

                      {fileError && (
                        <Fade in>
                          <Alert
                            severity="error"
                            onClose={() => setFileError(null)}
                            sx={{ mt: 2 }}
                            icon={<ClearIcon />}
                          >
                            {fileError}
                          </Alert>
                        </Fade>
                      )}
                    </Box>

                    {/* Alerts */}
                    {error && (
                      <Fade in>
                        <Alert
                          severity="error"
                          onClose={() => dispatch(setError(null))}
                          sx={{
                            background: (theme) => `linear-gradient(135deg, ${theme.palette.error.main}15 0%, ${theme.palette.error.light}15 100%)`,
                            border: (theme) => `1.5px solid ${theme.palette.error.main}40`,
                          }}
                        >
                          {error}
                        </Alert>
                      </Fade>
                    )}

                    {success && (
                      <Fade in>
                        <Alert
                          severity="success"
                          sx={{
                            background: (theme) => `linear-gradient(135deg, ${theme.palette.success.main}15 0%, ${theme.palette.success.light}15 100%)`,
                            border: (theme) => `1.5px solid ${theme.palette.success.main}40`,
                            '& .MuiAlert-icon': {
                              color: 'success.main',
                            },
                          }}
                        >
                          ✨ Found matching candidate profiles! Scroll down to view results.
                        </Alert>
                      </Fade>
                    )}

                    {/* Action Buttons */}
                    <Stack
                      direction={{ xs: 'column', sm: 'row' }}
                      spacing={2}
                      sx={{ pt: 2 }}
                    >
                      <ActionButton
                        variant="contained"
                        size="large"
                        onClick={handleSubmit}
                        disabled={loading || !hasInput}
                        endIcon={loading ? undefined : <SearchIcon />}
                        sx={{
                          flex: 1,
                          py: 1.8,
                          fontSize: '1.05rem',
                          fontWeight: 600,
                        }}
                      >
                        {loading ? (
                          <Box sx={{ display: 'flex', direction: 'row', alignItems: 'center', gap: 1 }}>
                            <CircularProgress size={20} color="inherit" />
                            <span>Searching Profiles...</span>
                          </Box>
                        ) : (
                          'Search Candidates'
                        )}
                      </ActionButton>

                      <ActionButton
                        variant="outlined"
                        size="large"
                        onClick={handleReset}
                        disabled={loading || !hasInput}
                        startIcon={<ClearIcon />}
                        sx={{
                          flex: 1,
                          py: 1.8,
                          fontSize: '1.05rem',
                          fontWeight: 600,
                        }}
                      >
                        Clear Form
                      </ActionButton>
                    </Stack>
                  </Stack>
                </CardContent>
              </GradientCard>

              {/* Info Cards or Results Table */}
              {!hasInput && !candidates.length && (
                <Fade in timeout={800}>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    {[
                      { icon: '🔍', title: 'Advanced Search', desc: 'AI-powered candidate matching' },
                      { icon: '🎯', title: 'Smart Matching', desc: 'Find the best fits for your role' },
                      { icon: '⚡', title: 'Instant Results', desc: 'Get candidates in seconds' },
                    ].map((item, idx) => (
                      <Card
                        key={idx}
                        sx={{
                          flex: 1,
                          textAlign: 'center',
                          border: '1px solid',
                          borderColor: 'divider',
                          background: (theme) => theme.palette.mode === 'light'
                            ? 'rgba(255, 255, 255, 0.8)'
                            : 'rgba(255, 255, 255, 0.05)',
                          backdropFilter: 'blur(10px)',
                          transition: 'all 0.3s ease',
                          
                          '&:hover': {
                            transform: 'translateY(-4px)',
                            boxShadow: (theme) => theme.palette.mode === 'light'
                              ? '0 12px 24px rgba(63, 81, 181, 0.1)'
                              : '0 12px 24px rgba(99, 125, 234, 0.15)',
                          },
                        }}
                      >
                        <CardContent>
                          <Typography variant="h4" sx={{ mb: 1 }}>
                            {item.icon}
                          </Typography>
                          <Typography variant="h6" sx={{ mb: 1, fontWeight: 700 }}>
                            {item.title}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            {item.desc}
                          </Typography>
                        </CardContent>
                      </Card>
                    ))}
                  </Stack>
                </Fade>
              )}

              {/* Results Table */}
              {candidates.length > 0 && (
                <Fade in timeout={600}>
                  <Box>
                    <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
                      <Typography
                        variant="h5"
                        sx={{
                          fontWeight: 700,
                          color: 'text.primary',
                          display: 'flex',
                          alignItems: 'center',
                          gap: 1,
                        }}
                      >
                        👥 Found {candidates.length} Matching Candidate{candidates.length !== 1 ? 's' : ''}
                      </Typography>
                      <Chip
                        label={`${candidates.length} profiles`}
                        color="primary"
                        variant="outlined"
                        sx={{ fontWeight: 600 }}
                      />
                    </Box>
                    
                    <CandidateResultsTable candidates={candidates} loading={loading} />

                    <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
                      <Button
                        variant="outlined"
                        onClick={handleReset}
                        fullWidth
                        sx={{
                          textTransform: 'none',
                          fontWeight: 600,
                          py: 1.5,
                        }}
                      >
                        New Search
                      </Button>
                      <Button
                        variant="contained"
                        fullWidth
                        sx={{
                          textTransform: 'none',
                          fontWeight: 600,
                          py: 1.5,
                          background: (theme) => `linear-gradient(135deg, ${theme.palette.primary.main} 0%, ${theme.palette.primary.dark} 100%)`,
                        }}
                      >
                        Export Results
                      </Button>
                    </Stack>
                  </Box>
                </Fade>
              )}
            </Stack>
          </Box>
        </Fade>
      </Container>
    </Box>
  );
};

export default DocumentUploadPage;
