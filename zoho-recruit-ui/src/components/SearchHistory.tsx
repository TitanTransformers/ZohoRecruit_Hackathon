import React from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Stack,
  Chip,
  IconButton,
  Tooltip,
  Collapse,
  List,
  ListItemButton,
  ListItemText,
  Divider,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import DeleteIcon from '@mui/icons-material/Delete';
import HistoryIcon from '@mui/icons-material/History';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import type { SearchHistoryItem } from '../services/searchHistoryService';

const HistoryCard = styled(Card)(() => ({
  background: 'rgba(255, 255, 255, 0.018)',
  border: '1px solid rgba(255, 255, 255, 0.07)',
  backdropFilter: 'blur(20px) saturate(160%)',
  borderRadius: '8px',
  transition: 'border-color 200ms ease, box-shadow 200ms ease',
  '&:hover': {
    borderColor: 'rgba(184, 147, 90, 0.3)',
    boxShadow: '0 8px 32px rgba(184, 147, 90, 0.1)',
  },
}));

const HistoryListItem = styled(ListItemButton)(() => ({
  borderRadius: '4px',
  marginBottom: '4px',
  transition: 'background-color 200ms ease',
  '&:hover': {
    background: 'rgba(184, 147, 90, 0.06)',
  },
}));

interface SearchHistoryProps {
  items: SearchHistoryItem[];
  onSelect: (item: SearchHistoryItem) => void;
  onDelete: (id: string) => void;
  onClear: () => void;
}

const SearchHistory: React.FC<SearchHistoryProps> = ({
  items,
  onSelect,
  onDelete,
  onClear,
}) => {
  const [expanded, setExpanded] = React.useState(false);

  if (items.length === 0) {
    return null;
  }

  const formatTime = (timestamp: number) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  return (
    <HistoryCard elevation={0}>
      <CardContent sx={{ p: { xs: 2, sm: 3 }, '&:last-child': { pb: { xs: 2, sm: 3 } } }}>
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            cursor: 'pointer',
            mb: expanded ? 2 : 0,
          }}
          onClick={() => setExpanded(!expanded)}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <HistoryIcon sx={{ color: '#B8935A' }} />
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 600, fontFamily: 'var(--font-display)', letterSpacing: '0.02em', color: '#EAE8E2' }}>
                Recent Searches
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {items.length} search{items.length !== 1 ? 'es' : ''}
              </Typography>
            </Box>
          </Box>
          <IconButton
            size="small"
            sx={{ transition: 'transform 0.3s ease' }}
          >
            {expanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
          </IconButton>
        </Box>

        <Collapse in={expanded} timeout="auto" unmountOnExit>
          <Box sx={{ mt: 2 }}>
            <List sx={{ p: 0 }}>
              {items.slice(0, 5).map((item, idx) => (
                <React.Fragment key={item.id}>
                  {idx > 0 && <Divider sx={{ my: 1 }} />}
                  <HistoryListItem
                    onClick={() => onSelect(item)}
                    sx={{ px: 1.5, py: 1 }}
                  >
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                          <Typography variant="body2" sx={{ fontWeight: 600, flex: 1, minWidth: 0 }}>
                            {item.query.substring(0, 50)}{item.query.length > 50 ? '...' : ''}
                          </Typography>
                          <Chip
                            label={item.type}
                            size="small"
                            variant="outlined"
                            sx={{ ml: 'auto', flexShrink: 0 }}
                          />
                        </Box>
                      }
                      secondary={
                        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', mt: 0.5 }}>
                          <Typography variant="caption" color="text.secondary">
                            {formatTime(item.timestamp)}
                          </Typography>
                          {item.candidatesCount !== undefined && (
                            <>
                              <Typography variant="caption" color="text.secondary">
                                •
                              </Typography>
                              <Typography variant="caption" color="text.secondary">
                                {item.candidatesCount} matches
                              </Typography>
                            </>
                          )}
                        </Box>
                      }
                    />
                    <Tooltip title="Delete">
                      <IconButton
                        edge="end"
                        size="small"
                        onClick={(e) => {
                          e.stopPropagation();
                          onDelete(item.id);
                        }}
                        sx={{
                          ml: 1,
                          color: 'error.main',
                          '&:hover': { background: 'rgba(239, 68, 68, 0.1)' },
                        }}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </HistoryListItem>
                </React.Fragment>
              ))}
            </List>

            {items.length > 5 && (
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2 }}>
                Showing 5 of {items.length} searches
              </Typography>
            )}

            <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
              <Button
                variant="outlined"
                size="small"
                onClick={onClear}
                sx={{ flex: 1, textTransform: 'uppercase', letterSpacing: '0.08em', fontSize: '0.7rem', borderColor: 'rgba(255,255,255,0.07)', color: '#6C6A7E', '&:hover': { borderColor: '#B8935A', color: '#EAE8E2' } }}
              >
                Clear All
              </Button>
            </Stack>
          </Box>
        </Collapse>
      </CardContent>
    </HistoryCard>
  );
};

export default SearchHistory;
