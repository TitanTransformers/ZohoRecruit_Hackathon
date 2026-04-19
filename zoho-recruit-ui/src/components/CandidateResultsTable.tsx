import React from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Box,
  Typography,
  LinearProgress,
  Tooltip,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import type { CandidateProfile } from '../types/candidate';

// Styled Components
const ResultsTableContainer = styled(TableContainer)(({ theme }) => ({
  marginTop: theme.spacing(3),
  borderRadius: theme.spacing(1.5),
  background: theme.palette.mode === 'light'
    ? 'rgba(255, 255, 255, 0.95)'
    : 'rgba(255, 255, 255, 0.05)',
  backdropFilter: 'blur(10px)',
  border: `1px solid ${theme.palette.divider}`,
}));

const StyledTableHead = styled(TableHead)(({ theme }) => ({
  '& .MuiTableCell-head': {
    background: `linear-gradient(135deg, ${theme.palette.primary.main}20 0%, ${theme.palette.primary.main}10 100%)`,
    fontWeight: 700,
    color: theme.palette.primary.main,
    borderBottom: `2px solid ${theme.palette.primary.main}40`,
    whiteSpace: 'nowrap',
  },
}));

const StyledTableRow = styled(TableRow)(({ theme }) => ({
  transition: 'all 0.2s ease',
  '&:hover': {
    background: theme.palette.mode === 'light'
      ? 'rgba(63, 81, 181, 0.05)'
      : 'rgba(99, 125, 234, 0.08)',
  },
  '& .MuiTableCell-body': {
    padding: theme.spacing(2),
  },
}));

const SkillChip = styled(Chip)(({ theme }) => ({
  margin: theme.spacing(0.5),
  fontSize: '0.85rem',
  fontWeight: 500,
}));

const MatchPercentageCell = styled(Box)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  gap: theme.spacing(1),
  minWidth: '150px',
}));

interface CandidateResultsTableProps {
  candidates: CandidateProfile[];
  loading?: boolean;
}

/**
 * Candidate Results Table Component
 * Displays candidate profiles in a formatted tabular layout
 */
const CandidateResultsTable: React.FC<CandidateResultsTableProps> = ({
  candidates,
  loading = false,
}) => {
  if (!candidates || candidates.length === 0) {
    return null;
  }

  const getMatchPercentageColor = (percentage: number) => {
    if (percentage >= 80) return 'success';
    if (percentage >= 60) return 'warning';
    return 'error';
  };

  const truncateText = (text: string, maxLength: number = 150): string => {
    if (text.length <= maxLength) return text;
    return `${text.substring(0, maxLength)}...`;
  };

  return (
    <ResultsTableContainer>
      {loading && (
        <LinearProgress
          sx={{
            height: 4,
            borderRadius: 1.5,
            background: 'rgba(63, 81, 181, 0.1)',
            '& .MuiLinearProgress-bar': {
              borderRadius: 1.5,
              background: (theme) => `linear-gradient(90deg, ${theme.palette.primary.main}, ${theme.palette.primary.light})`,
            },
          }}
        />
      )}
      <Table stickyHeader={loading}>
        <StyledTableHead>
          <TableRow>
            <TableCell align="left" sx={{ minWidth: '150px' }}>
              Name
            </TableCell>
            <TableCell align="left" sx={{ minWidth: '200px' }}>
              Email
            </TableCell>
            <TableCell align="left" sx={{ minWidth: '250px' }}>
              Matched Skills
            </TableCell>
            <TableCell align="left" sx={{ minWidth: '250px' }}>
              Missing Skills
            </TableCell>
            <TableCell align="left" sx={{ minWidth: '250px' }}>
              Analysis
            </TableCell>
            <TableCell align="center" sx={{ minWidth: '180px' }}>
              Match Percentage
            </TableCell>
          </TableRow>
        </StyledTableHead>
        <TableBody>
          {candidates.map((candidate, rowIndex) => (
            <StyledTableRow key={rowIndex}>
              {/* Name */}
              <TableCell align="left">
                <Typography
                  variant="body2"
                  sx={{ fontWeight: 600, color: 'text.primary' }}
                >
                  {candidate.name}
                </Typography>
              </TableCell>

              {/* Email */}
              <TableCell align="left">
                <Tooltip title={candidate.email}>
                  <Typography
                    variant="body2"
                    sx={{
                      color: 'primary.main',
                      textDecoration: 'none',
                      cursor: 'pointer',
                      '&:hover': { textDecoration: 'underline' },
                    }}
                    component="a"
                    href={`mailto:${candidate.email}`}
                  >
                    {truncateText(candidate.email, 40)}
                  </Typography>
                </Tooltip>
              </TableCell>

              {/* Matched Skills */}
              <TableCell align="left">
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {candidate.matchedSkill && candidate.matchedSkill.length > 0 ? (
                    candidate.matchedSkill.map((skill, idx) => (
                      <SkillChip
                        key={idx}
                        label={skill}
                        variant="filled"
                        color="success"
                        size="small"
                      />
                    ))
                  ) : (
                    <Typography variant="caption" color="text.secondary">
                      No matched skills
                    </Typography>
                  )}
                </Box>
              </TableCell>

              {/* Missing Skills */}
              <TableCell align="left">
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {candidate.missingSkills && candidate.missingSkills.length > 0 ? (
                    candidate.missingSkills.map((skill, idx) => (
                      <SkillChip
                        key={idx}
                        label={skill}
                        variant="filled"
                        color="warning"
                        size="small"
                      />
                    ))
                  ) : (
                    <Typography variant="caption" color="text.secondary">
                      No missing skills
                    </Typography>
                  )}
                </Box>
              </TableCell>

              {/* Analysis */}
              <TableCell align="left">
                <Tooltip title={candidate.analysis}>
                  <Typography
                    variant="caption"
                    sx={{
                      color: 'text.secondary',
                      display: '-webkit-box',
                      WebkitLineClamp: 3,
                      WebkitBoxOrient: 'vertical',
                      overflow: 'hidden',
                    }}
                  >
                    {truncateText(candidate.analysis, 150)}
                  </Typography>
                </Tooltip>
              </TableCell>

              {/* Match Percentage */}
              <TableCell align="center">
                <MatchPercentageCell>
                  <Box sx={{ width: '60px' }}>
                    <LinearProgress
                      variant="determinate"
                      value={candidate.matchedPercentage}
                      color={getMatchPercentageColor(candidate.matchedPercentage)}
                      sx={{
                        height: 8,
                        borderRadius: 4,
                        marginBottom: 0.5,
                      }}
                    />
                  </Box>
                  <Typography
                    variant="body2"
                    sx={{
                      fontWeight: 700,
                      color: `${getMatchPercentageColor(candidate.matchedPercentage)}.main`,
                      minWidth: '45px',
                    }}
                  >
                    {candidate.matchedPercentage.toFixed(1)}%
                  </Typography>
                </MatchPercentageCell>
              </TableCell>
            </StyledTableRow>
          ))}
        </TableBody>
      </Table>
    </ResultsTableContainer>
  );
};

export default CandidateResultsTable;
