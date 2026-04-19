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
  marginTop: theme.spacing(2),
  borderRadius: '12px',
  background: theme.palette.mode === 'light'
    ? 'rgba(255, 255, 255, 0.95)'
    : 'rgba(255, 255, 255, 0.04)',
  backdropFilter: 'blur(10px)',
  border: `1px solid ${theme.palette.divider}`,
  width: '100%',
  overflow: 'auto',
  boxShadow: theme.palette.mode === 'light'
    ? '0 4px 12px rgba(99, 102, 241, 0.08)'
    : '0 4px 12px rgba(99, 102, 241, 0.15)',
  [theme.breakpoints.down('sm')]: {
    borderRadius: '10px',
  },
}));

const StyledTableHead = styled(TableHead)(({ theme }) => ({
  '& .MuiTableCell-head': {
    background: `linear-gradient(135deg, ${theme.palette.primary.main}15 0%, ${theme.palette.primary.main}08 100%)`,
    fontWeight: 700,
    color: theme.palette.primary.main,
    borderBottom: `1.5px solid ${theme.palette.primary.main}30`,
    whiteSpace: 'nowrap',
    padding: theme.spacing(1.75, 2),
    fontSize: '0.95rem',
    [theme.breakpoints.down('sm')]: {
      fontSize: '0.85rem',
      padding: theme.spacing(1.5, 1),
    },
  },
}));

const StyledTableRow = styled(TableRow)(({ theme }) => ({
  transition: 'all 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
  '&:hover': {
    background: theme.palette.mode === 'light'
      ? 'rgba(99, 102, 241, 0.06)'
      : 'rgba(129, 140, 248, 0.1)',
  },
  '& .MuiTableCell-body': {
    padding: theme.spacing(1.75, 2),
    fontSize: '0.95rem',
    [theme.breakpoints.down('sm')]: {
      padding: theme.spacing(1.25, 1),
      fontSize: '0.85rem',
    },
  },
}));

const SkillChip = styled(Chip)(({ theme }) => ({
  margin: theme.spacing(0.5),
  fontSize: '0.8rem',
  fontWeight: 500,
  borderRadius: '6px',
  [theme.breakpoints.down('sm')]: {
    fontSize: '0.7rem',
    margin: theme.spacing(0.3),
  },
}));

const MatchPercentageCell = styled(Box)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  gap: theme.spacing(1),
  minWidth: '140px',
  [theme.breakpoints.down('sm')]: {
    minWidth: '100px',
  },
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
            height: 3,
            borderRadius: '12px 12px 0 0',
            background: 'rgba(99, 102, 241, 0.08)',
            '& .MuiLinearProgress-bar': {
              borderRadius: '12px',
              background: (theme) => `linear-gradient(90deg, ${theme.palette.primary.main}, ${theme.palette.primary.light})`,
            },
          }}
        />
      )}
      <Table stickyHeader={loading} sx={{ width: '100%', tableLayout: 'auto' }}>
        <StyledTableHead>
          <TableRow>
            <TableCell align="left" sx={{ minWidth: { xs: '120px', sm: '150px' } }}>
              Name
            </TableCell>
            <TableCell align="center" sx={{ minWidth: { xs: '110px', sm: '160px' } }}>
              Match %
            </TableCell>
            <TableCell align="left" sx={{ minWidth: { xs: '140px', sm: '200px' }, display: { xs: 'none', sm: 'table-cell' } }}>
              Email
            </TableCell>
            <TableCell align="left" sx={{ minWidth: { xs: '100px', sm: '220px' } }}>
              Matched Skills
            </TableCell>
            <TableCell align="left" sx={{ minWidth: { xs: '100px', sm: '220px' }, display: { xs: 'none', md: 'table-cell' } }}>
              Missing Skills
            </TableCell>
            <TableCell align="left" sx={{ minWidth: { xs: '90px', sm: '200px' }, display: { xs: 'none', lg: 'table-cell' } }}>
              Analysis
            </TableCell>
          </TableRow>
        </StyledTableHead>
        <TableBody>
          {candidates.map((candidate, rowIndex) => {
            const matchedPercentage = candidate.matchedPercentage ?? 0;
            const analysisText = candidate.analysis ?? '';

            return (
            <StyledTableRow key={rowIndex}>
              {/* Name */}
              <TableCell align="left">
                <Tooltip title={candidate.name}>
                  <Typography
                    variant="body2"
                    sx={{
                      fontWeight: 700,
                      color: 'text.primary',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {candidate.name}
                  </Typography>
                </Tooltip>
              </TableCell>

              {/* Match Percentage */}
              <TableCell align="center">
                <MatchPercentageCell sx={{ flexDirection: 'column', gap: 0.5 }}>
                  <Box sx={{ width: '100%' }}>
                    <LinearProgress
                      variant="determinate"
                      value={matchedPercentage}
                      color={getMatchPercentageColor(matchedPercentage)}
                      sx={{
                        height: 6,
                        borderRadius: 3,
                      }}
                    />
                  </Box>
                  <Typography
                    variant="body2"
                    sx={{
                      fontWeight: 700,
                      color: `${getMatchPercentageColor(matchedPercentage)}.main`,
                      fontSize: '0.85rem',
                    }}
                  >
                    {matchedPercentage.toFixed(0)}%
                  </Typography>
                </MatchPercentageCell>
              </TableCell>

              {/* Email - Hidden on mobile */}
              <TableCell align="left" sx={{ display: { xs: 'none', sm: 'table-cell' } }}>
                <Tooltip title={candidate.email}>
                  <Typography
                    variant="body2"
                    sx={{
                      color: 'primary.main',
                      textDecoration: 'none',
                      cursor: 'pointer',
                      '&:hover': { textDecoration: 'underline' },
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                    component="a"
                    href={`mailto:${candidate.email}`}
                  >
                    {truncateText(candidate.email, 35)}
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

              {/* Missing Skills - Hidden on mobile */}
              <TableCell align="left" sx={{ display: { xs: 'none', md: 'table-cell' } }}>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.4 }}>
                  {candidate.missingSkills && candidate.missingSkills.length > 0 ? (
                    candidate.missingSkills.slice(0, 2).map((skill, idx) => (
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
                      —
                    </Typography>
                  )}
                  {candidate.missingSkills && candidate.missingSkills.length > 2 && (
                    <Chip
                      label={`+${candidate.missingSkills.length - 2}`}
                      size="small"
                      variant="outlined"
                      sx={{ fontSize: '0.7rem' }}
                    />
                  )}
                </Box>
              </TableCell>

              {/* Analysis - Hidden on smaller screens */}
              <TableCell align="left" sx={{ display: { xs: 'none', lg: 'table-cell' } }}>
                <Tooltip title={analysisText}>
                  <Typography
                    variant="caption"
                    sx={{
                      color: 'text.secondary',
                      display: '-webkit-box',
                      WebkitLineClamp: 2,
                      WebkitBoxOrient: 'vertical',
                      overflow: 'hidden',
                    }}
                  >
                    {truncateText(analysisText, 120)}
                  </Typography>
                </Tooltip>
              </TableCell>

            </StyledTableRow>
            );
          })}
        </TableBody>
      </Table>
    </ResultsTableContainer>
  );
};

export default CandidateResultsTable;
